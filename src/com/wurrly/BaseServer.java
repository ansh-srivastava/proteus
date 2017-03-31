/**
 * 
 */
package com.wurrly;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jsoniter.DecodingMode;
import com.jsoniter.JsonIterator;
import com.jsoniter.ReflectionDecoderFactory;
import com.jsoniter.annotation.JsoniterAnnotationSupport;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsoniterSpi;
import com.wurrly.controllers.Users;
import com.wurrly.models.User;
import com.wurrly.modules.DIModule;
import com.wurrly.utilities.HandleGenerator;
import com.wurrly.utilities.ServerRequest;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

/**
 * @author jbauer
 */
public class BaseServer
{
	
	public interface RouteArgumentMapper<M,E>
	{
		public void map(M method, E exchange);
	}
	
	private static Logger Logger = LoggerFactory.getLogger(BaseServer.class.getCanonicalName());

	private static final String URL_ENCODED_FORM_TYPE = "x-www-form-urlencoded";
	private static final String FORM_DATA_TYPE = "form-data";
	private static final String OCTET_STREAM_TYPE = "octet-stream";
	private static final String JSON_TYPE = "application/json";
	private static final String CONTENT_TYPE = "Content-Type";
	static final String CHARSET = "UTF-8";
	
 
	
	public static class BaseHandlers
	{
		 public static void notFoundHandler(HttpServerExchange exchange) {
		        exchange.setStatusCode(404);
		        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
		        exchange.getResponseSender().send("Page Not Found!!");
		    }
	}
	
	public static RouteArgumentMapper<Method,HttpServerExchange> mapMethodParameters =  (Method method, HttpServerExchange exchange) ->
	{
        final Parameter[] parameters = method.getParameters();
        
        for (final Parameter parameter : parameters)
        {
        	Logger.debug("parameter: " + parameter.getParameterizedType() + " " + parameter.getName());
        	
        	if( parameter.getAnnotatedType() instanceof ServerRequest)
        	{
        		
        	}
        } 
	};
	 
	 /*
	  * Undertow server = Undertow.builder()
    .addHttpListener(8080, "0.0.0.0")
    .setHandler(Handlers.pathTemplate()
        .add("/item/{itemId}", new ItemHandler())
    )
    .build();
server.start();
	  */
	
	 static Optional<String> queryParam(HttpServerExchange exchange, String name) {
	       
	        return Optional.ofNullable(exchange.getQueryParameters().get(name))
	                       .map(Deque::getFirst);
	    }
 

	    static Optional<String> pathParam(HttpServerExchange exchange, String name) {
	        
	        return Optional.ofNullable(exchange.getQueryParameters().get(name))
	                       .map(Deque::getFirst);
	    }

	    static Optional<Long> pathParamAsLong(HttpServerExchange exchange, String name) {
	        return pathParam(exchange, name).map(Long::parseLong);
	    }

	    static Optional<Integer> pathParamAsInteger(HttpServerExchange exchange, String name) {
	        return pathParam(exchange, name).map(Integer::parseInt);
	    } 
	    
	
	    final static ByteBuffer msgBuffer  = ByteBuffer.wrap("hello world".getBytes());
 	
	public static void main(String[] args)
	{

		try
		{
			
		    Injector injector = Guice.createInjector(new DIModule());

 			
			Users usersController = injector.getInstance(Users.class);
			
		 //   injector.injectMembers(usersController);

			
			JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);
			JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
			JsoniterAnnotationSupport.enable();

			RoutingHandler router = new RoutingHandler().setFallbackHandler(BaseHandlers::notFoundHandler);
			
//			HttpHandler getUserHandler = null;
//			GetUsersHandler getUserHandler = new GetUsersHandler(usersController);
			
			for( Method m : Users.class.getDeclaredMethods() )
			{
				System.out.println("method: " + m);
				
				if( m.isSynthetic() || !m.getDeclaringClass().equals(Users.class))
				{
					System.out.println("m " + m + " is shady");
					continue;
				}
				
 				HttpString httpMethod = HandleGenerator.extractHttpMethod.apply(m);
				String pathTemplate = HandleGenerator.extractPathTemplate.apply(m);
				HttpHandler handler = HandleGenerator.generateHandler(usersController, m, httpMethod.equals(Methods.POST));

				Logger.info("\nFUNCTION: " + m + "\n\tMETHOD: " + httpMethod + "\n\tPATH: " + pathTemplate);
 	 			
				
	 			router.add(httpMethod, pathTemplate,  handler );

	 		//	router.addAll(Handlers.path().addPrefixPath(pathTemplate, handler));
				System.out.println("handler: " + handler);
				 
			}
			
//			 final HttpHandler createUserPostHandler = new HttpHandler() {
//				    @Override
//				    public void handleRequest(final HttpServerExchange exchange) throws Exception {
//				      if(exchange.isInIoThread()) {
//				        exchange.dispatch(this);
//				      }
//				      ServerRequest serverRequest = new ServerRequest(exchange);
//				      Optional<String> context = Optional.ofNullable(exchange.getQueryParameters().get("context")).map(Deque::getFirst);
//				      Any user = exchange.getAttachment(ServerRequest.REQUEST_JSON_BODY).readAny();
//				      Any response = usersController.createUser(serverRequest,context,user);
//				      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
//				      exchange.getResponseSender().send(com.jsoniter.output.JsonStream.serialize(response));
//				    }
//				  };
			
			router.add(Methods.GET, "/", new HttpHandler(){

				/* (non-Javadoc)
				 * @see io.undertow.server.HttpHandler#handleRequest(io.undertow.server.HttpServerExchange)
				 */
				@Override
				public void handleRequest(HttpServerExchange exchange) throws Exception
				{
					// TODO Auto-generated method stub
					
					exchange.getResponseSender().send(msgBuffer);  
					
				}
				
				
				
			} );
			
 		 
			Undertow server = Undertow.builder()
					.addHttpListener(8090, "localhost")
					.setBufferSize(1024 * 16)
					.setIoThreads(Runtime.getRuntime().availableProcessors())
					.setServerOption(UndertowOptions.ENABLE_HTTP2, true)
			        .setServerOption(UndertowOptions.ALWAYS_SET_DATE, true) 
			        .setSocketOption(org.xnio.Options.BACKLOG, 10000)
			        .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, false)

					.setWorkerThreads(Runtime.getRuntime().availableProcessors() * 2)
					.setHandler(new HttpHandler()
			{
				@Override
				public void handleRequest(final HttpServerExchange exchange) throws Exception
				{
					try
					{
//						if(exchange.isInIoThread())
//						{
//							exchange.dispatch(this);
//							return;
//						}
						 
						
						router.handleRequest(exchange);
						
					} catch (Exception e)
					{
						if (exchange.isResponseChannelAvailable())
						{
							e.printStackTrace();
						}
					}
				}
			}).build();
			server.start();
			
			Runtime.getRuntime().addShutdownHook( new Thread(){
			 
				@Override
				public void run()
				{
					 
				}
			});


		} catch (Exception e)
		{
			e.printStackTrace();
		}

	}
	
	 
	
	public static class UserIdHandler implements HttpHandler {

	    @Override
	    public void handleRequest(HttpServerExchange exchange) throws Exception {
	    	
 
//	      // Method 1
//	      PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
//	      
//	      System.out.println(pathMatch.getMatchedTemplate());
//	      
//	      System.out.println(pathMatch.getParameters());
//	      
//
//	      System.out.println(exchange.getQueryParameters());
	    	
	    	

	      Long userId = pathParam(exchange,"id").map(Long::parseLong).orElse(0l); 
	      
	      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, JSON_TYPE); 
	      
	      User user = new User();
	      user.setId(userId);
//
       String json = JsonStream.serialize(user);
	 
       	//	JsonStream.serialize(user,exchange.getOutputStream());
	     exchange.getResponseSender().send(json);  

	     // exchange.getResponseSender().send(ByteBuffer.wrap(JsonMapper.getInstance().writeValueAsBytes(user)));  
 	      
 	      //  	      exchange.getResponseSender().send(JsonStream.serialize(Collections.singletonMap("id", userId)));  

	    }
	}

}
