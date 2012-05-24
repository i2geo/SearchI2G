package net.i2geo.search;

import net.i2geo.index.rsearch.RSearchContext;
import net.i2geo.index.rsearch.RSearchQueryExpander;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

/**
 */
public class CurrikiQueryExpanderService extends QueryExpanderBaseServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            RSearchContext context = getSearchContext(req);
            RSearchQueryExpander exp = new RSearchQueryExpander(context,indexHome,onto);
            exp.setFieldNames("CurrikiCode.AssetClass.eduLevelFine","CurrikiCode.AssetClass.trainedTopicsAndCompetencies");
            String simpleQuery = req.getParameter("terms");
            String language = req.getParameter("language");
            String special = req.getParameter("special"); // TODO which values for special?

            BooleanQuery terms = exp.peelTerms(simpleQuery);
            //exp.recognizeGSNodesInTextQueries(terms);
            Query query = exp.expandToLuceneQuery(terms);
            sanitizeQuery(query);
            /*BooleanQuery query = new BooleanQuery();
               query.add(new TermQuery(
               new Term("CurrikiCode.AssetClass.trainedTopicsAndCompetencies","Know_theorem_about_sum_of_angles_in_triangle")),
               BooleanClause.Occur.SHOULD); */
            // TODO: use language
            req.setAttribute("luceneQuery",query);
            String queryText = query.toString();
            req.setAttribute("luceneQueryText",queryText);
            System.out.println("Have expanded query to "+queryText+ ".");

            ServletContext xwikiC = getServletContext().getContext("/xwiki");
            if(req.getParameter("target")!=null) {
                RequestDispatcher d= xwikiC.getRequestDispatcher(req.getParameter("target"));
                d.forward(req,resp);
            } else {
                RequestDispatcher d= xwikiC.getRequestDispatcher("/bin/view/Search/Resources");
                d.forward(req,resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    private void sanitizeQuery(Query query) {
        if(query instanceof BooleanQuery) {
            BooleanQuery bq = (BooleanQuery) query;
            
        }
    }

}
