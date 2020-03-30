<%@page import="java.net.InetAddress"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="sssii.billing.server.*" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Billing Server</title>
    <link href="css/style.css" rel="stylesheet" type="text/css" />
    <link href="favicon.ico" rel="shortcut icon" />
</head>

<body>
<h2>Billing server</h2>
<%
   String hostname = InetAddress.getLocalHost().getCanonicalHostName();
%>
<p>
<%= hostname %>

<div class="smallergray">
    <jsp:useBean id="helper" class="sssii.billing.server.AppPropertiesHelper" scope="application"></jsp:useBean>
<%--
Version <jsp:getProperty name="helper" property="version"/> <jsp:getProperty name="helper" property="build"/>
 --%>
<jsp:getProperty name="helper" property="htmlBuildAndVersion"/>
</div>

</body>
</html>
