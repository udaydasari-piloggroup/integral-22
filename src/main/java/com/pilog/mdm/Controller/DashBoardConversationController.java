package com.pilog.mdm.Controller;

import javax.servlet.http.HttpServletRequest;

import com.pilog.mdm.Service.DashBoardsConversationService;
import com.pilog.mdm.Service.DashBoardsService;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
*
* @author Jagadish.R
*/

@Controller
public class DashBoardConversationController {
	@Autowired
	public DashBoardsConversationService dashBoardsService;
	
	@RequestMapping(value = "/getConversationalAIMessage", produces = { "application/json" }) 
    public @ResponseBody JSONObject getConversationalAIMessage(HttpServletRequest request) {    
    	JSONObject resultObj = new JSONObject();
    	try {
    		resultObj = dashBoardsService.getConversationalAIMessage(request);           
    	} catch (Exception e) {             
    		e.printStackTrace(); 
    	}
    	
    	return resultObj;
    }
	
	@RequestMapping(value = "/getUserTableNamesData", method = { RequestMethod.GET, RequestMethod.POST })
   	public @ResponseBody JSONObject getUserTableNames(HttpServletRequest request) {    
   		JSONObject resultObject = null;
   		try {
   			resultObject = dashBoardsService.getUserTableNames(request);                           
   		} catch (Exception e) { 
   			e.printStackTrace();
   		}
   		return resultObject;  
   	}
	
	@RequestMapping(value = "/getUserMergeTableNamesData", method = { RequestMethod.GET, RequestMethod.POST })
   	public @ResponseBody JSONObject getUserMergeTableNames(HttpServletRequest request) {    
   		JSONObject resultObject = null;
   		try {
   			resultObject = dashBoardsService.getUserMergeTableNames(request);                            
   		} catch (Exception e) { 
   			e.printStackTrace();
   		}
   		return resultObject;  
   	}
	@RequestMapping(value = "/getUserMergeTableNamesColumns", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getUserMergeTableNamesColumns(HttpServletRequest request) {    
		JSONObject resultObject = null;
		try {
			resultObject = dashBoardsService.getUserMergeTableNamesColumns(request);                           
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return resultObject;  
	}
	
	@RequestMapping(value = "/getUserSearchData", method = { RequestMethod.GET, RequestMethod.POST })
   	public @ResponseBody JSONObject getChatBotResponse(HttpServletRequest request) {
   		JSONObject resultObject = null;
   		try {
   			resultObject = dashBoardsService.getUserSearchData(request);
   		} catch (Exception e) {
   			e.printStackTrace();
   		}
   		return resultObject;  
   	}
	
	
	@RequestMapping(value = { "/getVoiceSuggestedChartsBasedonColumns" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getVoiceSuggestedChartsBasedonColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getVoiceSuggestedChartsBasedonColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	@RequestMapping(value = "/getInsightsView", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getInsightsView(HttpServletRequest request) {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject = dashBoardsService.getInsightsView(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;  
	}
	@RequestMapping(value = "/executeInsightsSQLQuery", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject executeInsightsSQLQuery(HttpServletRequest request) {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject = dashBoardsService.executeInsightsSQLQuery(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;  
	}
}
