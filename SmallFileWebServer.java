package utils.magicjake.webfileserver;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Jacob Nordgren on 2016-09-28.
 */

public class SmallFileWebServer extends AsyncTask<String,Void,Void> {

    private int SERVER_PORT = 4711;

    private ServerSocket serverSocket;

    private ArrayList<Socket> activeConnections;

    private static final String TAG = "WebServer";

    private boolean running = false;

    private File rootDir;


    private String serverName = "WebSurvur";

    public SmallFileWebServer(String rootDir, int port){
        activeConnections = new ArrayList<>();
        this.rootDir = new File(rootDir);
        SERVER_PORT = port;
    }
    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG,"Unable to bind listening socket!");
        }
        running = true;
        while(running){
            try {
                Socket clientSocket = serverSocket.accept();
                activeConnections.add(clientSocket);

                this.handleRequest(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        return null;
    }

    /**
     * Returns a human readable string for the ip of the server.
     * @return
     */
    public String getServerIp(){
        return serverSocket.getInetAddress().toString();
    }
    @Override
    protected void onPostExecute(Void result) {

    }

    /**
     * Determine if String is alpha numeric or not
     * @param s string to be check
     * @return true if only alpha numeric otherwise false
     */
    private boolean isAlphaNumeric(String s){
        for(char c : s.toCharArray()){
            if( c =='.' || c=='_' )
                continue;
            if( (!Character.isLetterOrDigit(c)))
                return false;
        }
        return true;
    }
    /**
     *
     * @param s
     */
    private void handleRequest(Socket s){
        BufferedReader reader = null;
        PrintStream output = null;

        try {
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            output = new PrintStream(s.getOutputStream());

            String reqLine = "";
            while(TextUtils.isEmpty(reqLine = reader.readLine())){

                /* 'Parse' http request */
                if(reqLine.contains("GET /")){
                    String [] reqParts = reqLine.split("");
                    if( reqParts.length != 3)
                    {
                        printHttpError(output,HttpStatusCodes.BAD_REQUEST);
                        break;
                    }
                    if( reqParts[1].length() > 200 || !isAlphaNumeric(reqParts[1]) ){
                        printHttpError(output,HttpStatusCodes.BAD_REQUEST);
                        break;
                    }

                    respondWithFile(output,reqParts[1]);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    //ignored.
                }
            if(output != null)
            output.close();
            return;
        }



    }

    /**
     * Takes a file a generates a URL friendly string.
     * @param f
     * @return
     * @throws UnsupportedEncodingException
     */
    private String getUrlFriendlyId(File f) throws UnsupportedEncodingException {
        return URLEncoder.encode(f.getName(),"UTF-8");
    }
    /**
     * Return the index page of the server
     * @return  a string with the index page html
     */
    private String getIndexPage(){

        StringBuilder indexRsp = new StringBuilder();
        indexRsp.append("<html>" +
                "<head>" +
                "<title>"+serverName+"</title>" +
                "</head>" +
                "<body>");

        indexRsp.append("<ul>");
        for( File f : rootDir.listFiles()){
            try {
                if(f.isDirectory()){
                    indexRsp.append("<li><a href=\"/dir/"+this.getUrlFriendlyId(f)+"\"> "+f.getName()+"</a></li>");

                }else{
                    indexRsp.append("<li><a href=\"/file/"+this.getUrlFriendlyId(f)+"\"> "+f.getName()+"</a></li>");
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        indexRsp.append("</ul>");
        indexRsp.append("</body>" +
                "</html>");

        return indexRsp.toString();
    }

    private String getSuffix(String fileName){
        int lpos = fileName.lastIndexOf(".");
        return fileName.substring(lpos+1, fileName.length());
    }
    /**
     * Reads a file from local disk and sends to client.
     * @param out outpustream to wrtie to client
     * @param filename file to be sent.
     */
    private void respondWithFile(PrintStream out, String filename){


        switch(getSuffix(filename)){    // Handle filetypes.
            case FileTypes.HTML:
                break;
            case FileTypes.JPEG:
                break;
        }
        File f = new File(rootDir.getPath() + "/" + filename);
        try {

            FileInputStream fileInputStream = new FileInputStream(f);
            byte [] fileContent = new byte[(int) f.length()];
            fileInputStream.read(fileContent);

            printHttpReponse(out, fileContent,"application/octet-stream");


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            printHttpError(out,HttpStatusCodes.BAD_REQUEST);
        } catch (IOException e) {
            e.printStackTrace();
            printHttpError(out,HttpStatusCodes.BAD_REQUEST);
        }
    }
    private void printHttpError(PrintStream out, int httpError){

    }
    private void printHttpReponse(PrintStream out, byte[] rsp, String mime_type){
        out.println("HTTP/1.1 200 OK");
        out.println("Date: ");
        out.println("Content-Length: " + rsp.length);
        out.println("Connection: close");
        out.println("Content-Type: "+mime_type);
        out.println("");
        out.println(rsp);

    }
}
