package net.i2geo.search;

import net.i2geo.api.MatchingResourcesCounter;
import net.i2geo.index.IndexHome;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.*;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 */
public class IndexingMonitorServlet extends HttpServlet {

    Log log = LogFactory.getLog(IndexingMonitorServlet.class);

    public void init() {
        try {
            log.info("IndexHome starting.");
            indexHome = IndexHome.getInstance(getServletContext().getInitParameter("indexPath"));
            log.info("IndexHome initialized.");
        } catch(RuntimeException ex) {
            log.warn(ex);
            throw ex;
        }
    }

    IndexHome indexHome = null;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO: add a simple status
        if(request.getPathInfo().endsWith("log")) {
            deliverLog(request,response);
        } else if (request.getPathInfo().endsWith("doIndex")) {
            // TODO: protect requests to admin users here
            startIndexingProcess(request,response);
        } else {
            deliverSimpleStatus(request,response);
        }
    }

    protected void deliverLog(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        File pathToLog = indexHome.getLogFile();
        if(pathToLog==null) {
            response.setStatus(200);
            response.setContentType("text/plain;charset=utf-8");
            response.getWriter().println("No log is available, please come a bit later.");
        } else {
            long modifiedSince = 0;
            try {
                modifiedSince = request.getDateHeader("If-Modified-Since");
            }catch (IllegalArgumentException e) {
                log.warn("Wrong date header: " + request.getHeader("If-Modified-Since"));
            }
            if(modifiedSince>0 && pathToLog.lastModified() < modifiedSince) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            response.setStatus(200);
            response.setContentType("text/plain;charset=utf-8");
            response.setContentLength((int) pathToLog.length());
            OutputStream out = response.getOutputStream();
            byte[] buff = new byte[512];
            InputStream in = new FileInputStream(pathToLog);
            int l = 0;
            while((l=in.read(buff,0,512)) > 0)
                out.write(buff,0,l);
            out.flush();
        }

    }


    protected void startIndexingProcess(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String firstMessage = "Indexing started.";
        try {
            indexHome.setMatchingResourcesCounter((MatchingResourcesCounter) getServletContext().getAttribute(MatchingResourcesCounter.class.getName()));
            indexHome.startIndexingProcess();
        } catch(Exception ex) {
            log.error(ex);
            firstMessage = "Indexing starting failed: " + ex;
        }
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Indexing Start<title></head><body>");
        out.println("<h1>Indexing Start</h1>");
        out.println("<p>" + new Date() + "</p>");
        out.println("<p>" + firstMessage + "</p>");
        out.println("<p><a href=\"log\">please see log</a></p>");
        out.println("</body></html>");
    }

    protected void deliverSimpleStatus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.println("<html><head><title>Indexing Status<title></head><body>");
        out.println("<h1>Indexing Status</h1>");
        out.println("<p>" + new Date() + "</p>");
        out.println("<p>" + indexHome.reportStatus().replaceAll("\n","<br/>") + "</p>");
        out.println("<p><a href=\"log\">see last log</a></p>");
        out.println("<p><a href=\"doIndex\">start indexing process</a></p>");
        out.println("</body></html>");
    }
}
