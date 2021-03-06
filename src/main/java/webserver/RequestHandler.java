package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());
        
        // 자원을 다 사용하면 반드시 clsoe를 사용해줘야 하지만
        // try 절을 활용하면 clsoe를 쓰지 않아도 해당 자원의 사용이 모두 끝나면 clsoe를 해준다
        // closeable 인터페이스를 implement 하고 있기 때문
        
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
        	
        	// 클라이언트가 보내는 header를 line 단위로 읽기 위해 bufferedReader 를 사용
        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	String line = br.readLine();
        	
        	// null 값 무한 루프 방지, 빠른 종료 (test 코드로 인지)
        	if (line == null) {
        		return;
        	}
        	
        	String url = HttpRequestUtils.getUrl(line);
        	Map<String, String> headers = new HashMap<String, String>();
        	while(!"".equals(line)) {
        		log.debug("header : {}", line);
        		line = br.readLine();
        		String[] headerTokens = line.split(": ");
        		if (headerTokens.length == 2) {
        			headers.put(headerTokens[0], headerTokens[1]);
        		}
        	}
        	
        	log.debug("Content-length : {}", headers.get("Content-Length"));
        	
        	if (url.startsWith("/user/create")) {
        		String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
        		log.debug("Request Body : {}", requestBody);
        		Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
        		User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
        		log.debug("user : {}", user);
        		
        		url = "/index.html";
        	}
        	
        	
            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
