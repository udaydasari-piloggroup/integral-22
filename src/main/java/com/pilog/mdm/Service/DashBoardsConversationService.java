package com.pilog.mdm.Service;

import javax.servlet.http.HttpServletRequest;

import com.pilog.mdm.DAO.DashBoardsConversationDAO;
import com.pilog.mdm.utilities.PilogUtilities;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Jagadish.R
 */

@Service
public class DashBoardsConversationService {
	
	

	@Autowired
	public DashBoardsConversationDAO conversationDAO;

	public JSONObject getConversationalAIMessage(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = conversationDAO.getConversationalAIMessage(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getUserTableNames(HttpServletRequest request) {

		return conversationDAO.getUserTableNames(request);
	}

	public JSONObject getUserMergeTableNames(HttpServletRequest request) {

		return conversationDAO.getUserMergeTableNames(request);
	}

	public JSONObject getUserMergeTableNamesColumns(HttpServletRequest request) {

		return conversationDAO.getUserMergeTableNamesColumns(request);
	}

	public JSONObject getUserSearchData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String message = (String) request.getParameter("message");
			String userName = (String) request.getParameter("userName");
			String lang = (String) request.getParameter("lang");
			String mainDiv = "";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> inputMap = new LinkedMultiValueMap();
			inputMap.add("msg", message);
			inputMap.add("user_name", userName);
			inputMap.add("lang", lang);
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(inputMap,
					headers);
			RestTemplate template = new RestTemplate();
			ResponseEntity<JSONObject> response = template
					.postForEntity("http://idxp.pilogcloud.com:6653/google_search/", entity, JSONObject.class);
			JSONObject apiDataObj = response.getBody();

			resultObj.put("result", apiDataObj);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getVoiceSuggestedChartsBasedonColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = conversationDAO.getVoiceSuggestedChartsBasedonColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getInsightsView(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = conversationDAO.getInsightsView(request);
        } catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject executeInsightsSQLQuery(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = conversationDAO.executeInsightsSQLQuery(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
}
