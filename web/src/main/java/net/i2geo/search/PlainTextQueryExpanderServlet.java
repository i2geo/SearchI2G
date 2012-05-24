package net.i2geo.search;

import net.i2geo.index.rsearch.RSearchContext;
import net.i2geo.index.rsearch.RSearchQueryExpander;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

/**
 */
public class PlainTextQueryExpanderServlet extends QueryExpanderBaseServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RSearchContext context = getSearchContext(req);
        RSearchQueryExpander exp = new RSearchQueryExpander(context,indexHome,onto);
        String textQuery = req.getParameter("q");
        if(textQuery==null || textQuery.length()==0) {
            getServletContext().getRequestDispatcher("showEmptyQuery.jsp").forward(req,resp);
        } else {
            List<TermQuery> qs = new ArrayList<TermQuery>();
            for(String s: textQuery.split(" ")) {
                qs.add(new TermQuery(new Term("text",s)));
            }
            Query q = exp.expandToLuceneQuery(qs);
            req.setAttribute("luceneQ",q);
            req.setAttribute("userQ",textQuery);
            getServletContext().getRequestDispatcher("expandResult.jsp").forward(req,resp);
        }
    }

}
