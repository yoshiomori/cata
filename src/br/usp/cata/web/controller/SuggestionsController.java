package br.usp.cata.web.controller;

import java.util.HashSet;

import javax.management.RuntimeErrorException;
import javax.servlet.ServletContext;

import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.Post;
import br.com.caelum.vraptor.Resource;
import br.com.caelum.vraptor.Result;
import br.com.caelum.vraptor.interceptor.multipart.UploadedFile;

import br.usp.cata.model.AdviceFilter;
import br.usp.cata.model.Keyword;
import br.usp.cata.model.Languages;
import br.usp.cata.model.Opinion;
import br.usp.cata.service.OpinionService;
import br.usp.cata.service.PatternSuggestionPairService;
import br.usp.cata.service.RuleService;
import br.usp.cata.service.SourceService;
import br.usp.cata.service.UserService;
import br.usp.cata.util.Checker;
import br.usp.cata.util.FileProcessor;
import br.usp.cata.util.RulesTrees;
import br.usp.cata.util.TextAnalyzer;
import br.usp.cata.web.interceptor.IrrestrictAccess;


@Resource
@IrrestrictAccess
public class SuggestionsController {
	
	private final Result result;
	private final OpinionService opinionService;
	private final PatternSuggestionPairService patternSuggestionPairService;
	private final RuleService ruleService;
	private final SourceService sourceService;
	private final UserService userService;
	private final UserSession userSession;
	private final ServletContext servletContext;

	public SuggestionsController(Result result, OpinionService opinionService, PatternSuggestionPairService patternSuggestionPairService,
			RuleService ruleService, SourceService sourceService, UserService userService,
			UserSession userSession, ServletContext servletContext) {
		this.result = result;
		this.opinionService = opinionService;
		this.patternSuggestionPairService = patternSuggestionPairService;
		this.ruleService = ruleService;
		this.sourceService = sourceService;
		this.userService = userService;
		this.userSession = userSession;
		this.servletContext = servletContext;
	}

	/**
	 * A análise do arquivo é feita e as sugestões são geradas.
	 * @param file UploadedFile enviado pelo usuário para ser analisado.
	 * @param language Languages selecionado pelo usuário para que seu texto seja analisado.
	 * @param type String que especifica o tipo do arquivo no formato txt.
	 * @param adviceFilter AdviceFilter que seleciona o escopo das regras a serem usadas para a análise.
	 * @param filterIDs long[] que especifica o subconjunto de regras a ser usuado para a análise.
	 */
	@Post
	@Path("/suggestions/results")
	public void results(UploadedFile file, Languages language, String type, AdviceFilter adviceFilter, long[] filterIDs) {
		RulesTrees rulesTrees = new RulesTrees(ruleService, sourceService, userService, userSession);
		switch(adviceFilter) {
			case DEFAULT: 
				rulesTrees.buildDefaultTrees(language);
				break;
			case ALL:
				rulesTrees.buildAllTrees(language);
				break;
			case FILTERED_BY_USER:
				rulesTrees.buildUsersTrees(language, filterIDs);
				break;
			case FILTERED_BY_SOURCE:
				rulesTrees.buildSourcesTrees(language, filterIDs);
				break;
			default:
				break;
		}
		
		FileProcessor fileProcessor = new FileProcessor(file, type);// Formata o texto para ser processado pelo sistema.
		TextAnalyzer textAnalyzer = new TextAnalyzer(fileProcessor.getText(), language, servletContext);// Obtem os lemas e os tokens
		
		Checker checker = new Checker(textAnalyzer, rulesTrees, opinionService);
		result.include("numOfMistakes", checker.getNumOfMistakes());
		
		if(checker.getNumOfMistakes() != 0) {
			result.include("output", "Foram encontrados alguns problemas de estilo (estão destacados). Confira:");
			result.include("fileName", fileProcessor.getFileName());
			result.include("text", checker.getCheckedText());
			String textKeywords = "";
			for(String keyword : textAnalyzer.getKeywords())
				textKeywords += (";" + keyword);
			result.include("keywords", textKeywords);
		}
		else
			result.include("output", "Não há sugestões para o texto enviado.");
	}

	@Post
	@Path("suggestions/opinion/{data}")
	public void opinion(String data) {
		int i;
		
		//FIXME
		String type = "";
		for(i = 0; i < data.length() && data.charAt(i) != '|'; i++)
			type += data.charAt(i);
		if(type.equals("agree"))
			return;
		i++;	
		String id = "";
		for(; i < data.length() && data.charAt(i) != '|'; i++)
			id += data.charAt(i);
		long pairID = Long.parseLong(id);
		
		i++;
		String[] keywords = data.substring(i).split(";");
		
		Opinion opinion = new Opinion();
		HashSet<Keyword> keywordSet = new HashSet<Keyword>();	
		for(String keyword : keywords) {
			if(!keyword.equals("")) {
				Keyword kw = new Keyword();
				kw.setWord(keyword);
				kw.setOpinion(opinion);
				keywordSet.add(kw);
			}
		}		
		
		if(!keywordSet.isEmpty()) {
			opinion.setKeywords(keywordSet);
			opinion.setPatternSuggestionPair(patternSuggestionPairService.findById(pairID));
			
			opinionService.saveOrUpdate(opinion);
		}
	}
}
