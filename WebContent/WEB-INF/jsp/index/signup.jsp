<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>    
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<link href="<c:url value='/css/style.css'/>" rel="stylesheet" type="text/css" />
		<link href="<c:url value='/css/form.css'/>" rel="stylesheet" type="text/css" />
		<title>Crie uma conta</title>
	</head>
	<body>
		<div id="header">
			<div id="header-content">
				<div id="logo">
					<a href="<c:url value='/'/>" title='CATA'><b>C</b>ollaborative <b>A</b>cademic <b>T</b>ext <b>A</b>dvisor</a>
				</div>
				
				<div id="menu" class="nav_bar">
					<ul>
						<li><a href="<c:url value='/'/>" title='Início'>Início</a></li>
						<li><a href="<c:url value='/about'/>" title='Sobre'>Sobre</a></li>
						<li><a href="<c:url value='/advice'/>" title='Advice'>Advice</a></li>
					</ul>	
				</div>
			</div>
		</div>
		
		<div id="page">
			<div id="content">
				<form id="create-post-form" action="<c:url value='/signup'/>" method="post">
					<fieldset>
					<legend>Crie uma conta</legend>
					<div class="single_form_element">
						<label class="label" for="">Nome :</label>
						<input class="input_border input_width required" type="text" maxlength=100 name="newUser.name" value="${newUser.name}"/>
					</div>
					<div class="single_form_element">
						<label class="label" for="">Email :</label>
						<input class="input_border input_width required" type="text" maxlength=100 name="newUser.email" value="${newUser.email}"/>
					</div>
					<div class="single_form_element">
						<label class="label" for="">Senha :</label>
						<input class="input_border input_width required" type="password" maxlength=32 name="newUser.password" value="${newUser.password}"/>
					</div>
					<br />
					<input class="button" type="submit" value="Cadastrar minha conta">	
					</fieldset>			
				</form>		
			</div>
		</div>
		
<%@ include file="../footer.jsp"%>