package edu.miamioh.postonjw;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
	Jared Poston
	09/13/2017
	CSE 383
	HttpServer.java
	
	Server to handle HTTP GET and POST requests from clients, then responds accordingly
	
	Attributions
	
		I used this to find the most correct way to exit my program in the event of a server socket error.
		System.exit(1) was the defacto method for closing the JVM if an exception is encountered.
			https://stackoverflow.com/questions/37843506/how-can-i-safely-stop-this-java-server-program
			
		I used this to find the syntax for testing POST requests with curl from the command line.
		Also used this to find out I could test PUT requests with my server, to make sure it returned an error.
			https://superuser.com/questions/149329/what-is-the-curl-command-line-syntax-to-do-a-post-request
*/

public class HttpServer {

	InputStream in = null;
	OutputStream out = null;
	ServerSocket servSock = null;
	Socket clientSock = null;
	private static Logger LOGGER = Logger.getLogger("info");
	FileHandler fh = null;
	
	int port;

	public static void main(String[] args) {
		
		int port = -1;
		
		try {
			port = Integer.parseInt(args[0]);
		}
		// no port given
		catch(ArrayIndexOutOfBoundsException e) {
			System.err.println("Error opening server...No port provided");
			System.exit(1);
		}
		// non-integer port given
		catch(NumberFormatException e) {
			System.err.println("Error opening server...Non-integer port provided");
			System.exit(1);
		}
		HttpServer server = new HttpServer(port);
		server.Main();
	}

	public HttpServer(int port) {
		try {
			fh = new FileHandler("server.log");
			LOGGER.addHandler(fh);
			LOGGER.setUseParentHandlers(false);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
		} 
		catch (Exception e) {
			System.err.println("Error...can't open log file");
		}
		this.port = port;
	}

	public void Main() {
		
		// server initialization phase
		try {
			servSock = new ServerSocket(port);
			LOGGER.info("Opened server on port " + port + "\n");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "error opening server socket", e);
			System.exit(1); // exit if server socket cannot open during initialization phase
		}

		// server run phase
		while (true) {
			try {
				connect();
				String request = getRequest();
				processRequest(request);	
			} 
			catch (IOException e) {
				LOGGER.log(Level.SEVERE,"error during connection, disconnecting client", e);
				
				try {
					out.write(("HTTP/1.1 500 INTERNAL ERROR\r\nContent-Type: text/text\r\n\r\n" + 
					          "A server error occurred\r\n\r\n").getBytes());
					out.flush();
				} 
				catch (IOException e1) {
					LOGGER.log(Level.SEVERE, "error sending server error to client", e1);
				}
			}
			finally {
				
				try {
					// ending client connection to server
					clientSock.close();
				} 
				catch (IOException e) {
					LOGGER.log(Level.SEVERE, "error closing client connection", e);
				}
			}
		}
	}

	/*
	 * 1) Get the request type: POST, GET
	 * 2) Respond to client based on request type
	 */
	private void processRequest(String request) throws IOException {
		// split request for parsing
		
		String response = "";
		
		if (request.substring(0,4).contains("GET")) { // it's a GET request
			
			LOGGER.info("Received GET request from client:\n" + request);
			
			String[] getReq = request.split("\n");
			
			String path = getReq[0].substring(getReq[0].indexOf(" ") + 1, getReq[0].lastIndexOf(" "));
			
			String headers = "";
			
			for (int i = 1; i < getReq.length; i++) { // get header lines from request
				headers += getReq[i];
			}
			
			// headers
			response += "HTTP/1.1 200 OK\r\n";
			response += "Content-Type: text/text\r\n\r\n";
			
			// body
			response += "Jared Poston\r\n" + "postonjw\r\n";
			response += "Path: " + path + "\r\n";
			response += headers + "\r\n\r\n";
			
			out.write(response.getBytes());
		}
		else if (request.substring(0,4).contains("POST")) { // it's a POST request
			
			LOGGER.info("Received POST request from client:\n" + request);
			
			// headers
			response += "HTTP/1.1 200 OK\r\n";
			response += "Content-Type: text/text\r\n\r\n";
			
			// body
			response += "POST\r\n\r\n";
			
			out.write(response.getBytes());
		}
		else { // it's incorrect! Return HTTP error msg
			throw new IOException("Received invalid request from client");
		}
		out.flush();
	}

	// reads in the request sent by the client
	private String getRequest() throws IOException {
		String res = "";
		byte[] arr = new byte[10000];
		byte[] trimmed;
		
		int reqSize = in.read(arr); // reading into arr and storing the size of the request
		
		trimmed = new byte[reqSize];
		
		// storing in new array that is exact size of the request
		// prevents null values being stored in byte array
		for (int i = 0; i < reqSize; i++) {
			trimmed[i] = arr[i];
		}
		
		res = new String(trimmed);
		
		return res;
	}

	// accepts client connection with server
	public void connect() throws IOException {
		clientSock = servSock.accept();
		LOGGER.info("Client with IP: " + servSock.getInetAddress().getHostAddress() + " connected\n");
		
		// since these are declared as Input/OutputStream at the start, I am using the Input/OutputStream methods due to polymorphism
		in = new DataInputStream(clientSock.getInputStream());
		out = new DataOutputStream(clientSock.getOutputStream());

		clientSock.setSoTimeout(10000); // 10 second timeout for clients
	}
}
