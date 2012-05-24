package net.i2geo.search;

import net.i2geo.index.IndexHome;
import net.i2geo.index.SKBUpdateQueue;
import net.i2geo.index.SKBUpdater;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.io.File;

/** Receives requests to extend the indexed set of words
 */
public class SKBUpdaterServlet extends HttpServlet {

    private IndexHome indexHome = null;
    private SKBUpdateQueue updaterQueue = null;
    private SKBUpdater updater = null;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        updaterQueue.receiveUpdate(req.getContentType(),req.getInputStream());
        resp.setStatus(200);
        resp.setContentType("application/xml;charset=ascii");
        resp.getOutputStream().println("<ok/>");
        synchronized(updaterQueue)
            {updaterQueue.notify();} 
    }


    /** just gets a list of the current updates in store. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain;charset=utf-8");
        updaterQueue.dumpRemainingChanges(resp.getOutputStream());
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        this.indexHome = IndexHome.getInstance(servletConfig.getServletContext().getInitParameter("indexPath"));
        this.updaterQueue = new SKBUpdateQueue(
                new File(servletConfig.getServletContext().getInitParameter("updateQueueDir")),
                new File(servletConfig.getServletContext().getInitParameter("rejectsQueueDir")));
        this.updater = new SKBUpdater(updaterQueue,indexHome);
        this.updater.start(); 
    }

    @Override
    public void destroy() {
        this.updater.interrupt();
        super.destroy();
    }
}
