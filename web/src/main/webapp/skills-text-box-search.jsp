<%--
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<%
  // example: skbSearchField|Topic,Competency|choiceIdentified
  String title = request.getParameter("title");
  if(title==null) title = "Skills Text Box Search";
  String types = request.getParameter("types");
  if(types==null) types="";
  String setValueFunction = request.getParameter("setValueFunction");
  if(setValueFunction==null) setValueFunction="showNode";
    String paramLanguage = request.getParameter("language");
    String languages;
    if(paramLanguage ==null) languages = request.getHeader("Accept-Language");
    else languages = paramLanguage + ";" + request.getHeader("Accept-Language");
    String firstLanguage = languages;
    if(firstLanguage.contains(";")) firstLanguage = firstLanguage.substring(0,firstLanguage.indexOf(";"));
    if(firstLanguage.contains("-")) firstLanguage = firstLanguage.substring(0,firstLanguage.indexOf("-"));
%>
  <head>
      <title><%=title%></title>
      <script type="text/javascript">
          window.skbSearchPleaseReplaceMe =  "searchField| <%=types%> |<%=setValueFunction%>|true";
          window.skbConfigBasePath = "/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/";
          window.browserLanguages = "<%=languages%>";
      </script>
      <script language='javascript' src='/static/JStrans/JStrans-<%=firstLanguage%>.js'></script>
      <script language='javascript' src='/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/net.i2geo.skillstextbox.SkillsTextBox.nocache.js'></script>
      <link rel="stylesheet" type="text/css"  href="/SearchI2G/net.i2geo.skillstextbox.SkillsTextBox/SkillsTextBox.css" />
  </head>
  <body>


  <h3><%=title%></h3>
  <form action="" onsubmit="return false;">
      <div id="searchField"></div>
  </form>

  <script type="text/javascript">
      window.justAlert = new Function("a","b",
          "alert('choice identified :' + a + ' of type ' + b);");
      window.showNode = function doShowNode(uri,type) {
          // http://draft.i2geo.net/comped/showProcess.html?uri=Sketch_graph
          if(uri.charAt(0)=='#') uri = uri.substr(1);
          uri = encodeURIComponent(uri);
          if("topic" == type)
            window.location.href = "http://i2geo.net/comped/showTopic.html?uri=" + uri;
          else if ("competency"==type)
              window.location.href = "http://i2geo.net/comped/showCompetency.html?uri="  + uri;
          else if ("level"==type)
              window.location.href = "http://i2geo.net/ontologies/dev/individuals/" + uri;
          else alert("Don't know what to do with this type \"" + type + "\".");
      }
  </script>

  </body>
</html>