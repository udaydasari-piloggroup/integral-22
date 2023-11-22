package com.pilog.mdm.DAO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.*;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import com.pilog.mdm.access.DataAccess;
import com.pilog.mdm.utilities.PilogUtilities;


import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Jagadish.R
 */

@Repository
public class DashBoardsConversationDAO {

	@Value("${jdbc.username}")
	private String userName;
	@Value("${jdbc.password}")
	private String password;
	@Value("${jdbc.driver}")
	private String dataBaseDriver;
	@Value("${jdbc.url}")
	private String dbURL;

	@Autowired
	private DataAccess access;

	@Transactional
	public List getConversationalAIData(HttpServletRequest request) {
		List listData = new ArrayList();
		String messageId = request.getParameter("messageId");
		try {
			String selectQuery = "SELECT " + "MESSAGE_ID, "// 0
					+ "RIGHT_MESSAGE, "// 1
					+ "LEFT_MESSAGE, "// 2
					+ "RIGHT_BUTTON, "// 3
					+ "LEFT_BUTTON, "// 4
					+ "RIGHT_BUTTON_METHOD, "// 5
					+ "LEFT_BUTTON_METHOD, "// 6
					+ "RIGHT_NEXT_METHOD, "// 7
					+ "LEFT_NEXT_METHOD, "// 8
					+ "REPLIED_ID, "// 9
					+ "CUSTOM_COL1, "// 10
					+ "CUSTOM_COL2, "// 11
					+ "CUSTOM_COL3, "// 12
					+ "CUSTOM_COL4, "// 13
					+ "CUSTOM_COL5, "// 14
					+ "CUSTOM_COL6, "// 15
					+ "CUSTOM_COL7, "// 16
					+ "CUSTOM_COL8, "// 17
					+ "CUSTOM_COL9, "// 18
					+ "CUSTOM_COL10 "// 19
					+ "FROM CREATE_CHART_CONVERSATIONS WHERE MESSAGE_ID =:MESSAGE_ID";
			Map mapData = new HashMap();
			mapData.put("MESSAGE_ID", messageId);
			listData = access.sqlqueryWithParams(selectQuery, mapData);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return listData;

	}

	@Transactional
	public JSONObject getConversationalAIMessage(HttpServletRequest request) {
		JSONObject jsonData = new JSONObject();
		try {
			String mainDiv = "<div class='visionChartsAutoSuggestionsClass'>";
			String rightMainDIv = "";
			String leftMainDIv = "";
			List listData = getConversationalAIData(request);
			if (listData != null && !listData.isEmpty()) {
				mainDiv += "<div class='convai-message'>";
				rightMainDIv += "<div class='convai_right_main_message'>";
				leftMainDIv += "<div class='convai_left_main_message'>";
				for (int i = 0; i < listData.size(); i++) {
					Object[] objData = (Object[]) listData.get(i);
					if (objData != null) {
						int messageId = ((BigDecimal) objData[0]).intValue();
						String rightMsg = (String) objData[1];
						String leftMsg = (String) objData[2];
						String rightBtn = (String) objData[3];
						String leftBtn = (String) objData[4];
						String rightBtnMtd = (String) objData[5];
						String leftBtnMtd = (String) objData[6];
						String rightNxtMtd = (String) objData[7];
						String leftNxtMtd = (String) objData[8];
						int repliedId = 0;
						if (objData[9] != null) {
							repliedId = ((BigDecimal) objData[9]).intValue();
						}
						if (rightMsg != null && !"".equalsIgnoreCase(rightMsg)) {
							rightMainDIv += "<div class='visionConversationalAIClass convai-right-message nonLoadedBubble'>"
									+ rightMsg + "</div>";
						}
						if (leftMsg != null && !"".equalsIgnoreCase(leftMsg)) {
							leftMainDIv += "<div class='visionConversationalAIClass convai-left-message nonLoadedBubble'>"
									+ leftMsg + "</div>";
						}
						if (rightBtn != null && !"".equalsIgnoreCase(rightBtn)) {
							rightMainDIv += "<button class='visionConversationalAIClass convai-left-message-button nonLoadedBubble' onclick=\""
									+ rightBtnMtd + "\">" + rightBtn + "</button>";
						}
						if (leftBtn != null && !"".equalsIgnoreCase(leftBtn)) {
							leftMainDIv += "<button class='visionConversationalAIClass convai-left-message-button nonLoadedBubble' onclick=\""
									+ leftBtnMtd + "\">" + leftBtn + "</button>";
						}
						jsonData.put("rightNxtMtd", rightNxtMtd);
						jsonData.put("leftNxtMtd", leftNxtMtd);
						jsonData.put("replyId", repliedId);
						if (i == listData.size() - 1) {
							rightMainDIv += "</div>";
							leftMainDIv += "</div>";
						}
					}
				}
				mainDiv += leftMainDIv + rightMainDIv;
				mainDiv += "</div>";
			}
			mainDiv += "</div>";
			jsonData.put("mainDiv", mainDiv);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return jsonData;
	}

	@Transactional
	public JSONObject getUserTableNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String tableDiv = "";
			String userName = (String) request.getParameter("userName");
			String replyId = (String) request.getParameter("replyId");
			if (userName != null && !"".equalsIgnoreCase(userName)) {
				String fetchQuery = "SELECT TABLE_NAME  FROM C_ETL_DAL_AUTHORIZATION WHERE CREATE_BY =:CREATE_BY";
				Map mapData = new HashMap();
				mapData.put("CREATE_BY", userName);
				List listData = access.sqlqueryWithParams(fetchQuery, mapData);
				if (listData != null && !listData.isEmpty()) {
					tableDiv = "<div id='userTableNamesDivId' class='userTableNamesDivClass text-right replyIntelisenseView noBubble'>"
							// + "<p class='nonLoadedBubble'>Existing Files/Tables</p>"
							+ "<div class=\"search nonLoadedBubble\">"
							+ "<input type=\"text\" placeholder=\"search\" id='data-search'/>" + "</div>"
							+ "<div id='userIntellisenseViewTableNamesDivId' class='userIntellisenseViewTableNamesDivClass nonLoadedBubble'>";
					for (int i = 0; i < listData.size(); i++) {
						String tableName = (String) listData.get(i);
						tableDiv += "<div id='" + tableName
								+ "_table' class='userTableNameClass' onclick=getConversationalAISelectedDataTableName('"
								+ tableName + "','" + replyId
								+ "') data-intelliSenseViewTablefilter-item data-filter-name=\"" + tableName + "\">"
								+ tableName + "</div>";
					}
					tableDiv += "</div>" + "</div>";
				}
				resultObj.put("tableDiv", tableDiv);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getUserMergeTableNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			JSONArray tablesArr = new JSONArray();
			String tableDiv = "";
			String userName = (String) request.getParameter("userName");
			String replyId = (String) request.getParameter("replyId");
			if (userName != null && !"".equalsIgnoreCase(userName)) {
				String fetchQuery = "SELECT TABLE_NAME  FROM C_ETL_DAL_AUTHORIZATION WHERE CREATE_BY =:CREATE_BY";
				Map mapData = new HashMap();
				mapData.put("CREATE_BY", userName);
				List listData = access.sqlqueryWithParams(fetchQuery, mapData);
				if (listData != null && !listData.isEmpty()) {
					tableDiv = "<div id='userMergeTableNamesDivId' class='userTableNamesDivClass text-right replyIntelisenseView noBubble'>"
							+ "<div id='userIntellisenseViewMergeTableNamesDivId' class='userIntellisenseViewTableNamesDivClass nonLoadedBubble'>";
					for (int i = 0; i < listData.size(); i++) {
						String tableName = (String) listData.get(i);
						tablesArr.add(tableName);
					}
					tableDiv += "</div>"
							+ "<div id='userIntellisenseViewMergeTableNamesErrorDivId' class='userIntellisenseViewMergeTableNamesErrorDivClass'></div>"
							+ "<button id='userConservationalMergeTableNamesButtonId' value='Confirm' onclick='showConversationalMergeTableNames("
							+ replyId + ")'>Ok</button>" + "</div>";
				}
				resultObj.put("tableDiv", tableDiv);
				resultObj.put("tablesArr", tablesArr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getUserMergeTableNamesColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			JSONArray tablesArr = new JSONArray();
			String tableDiv = "";
			Map tablesObj = new LinkedHashMap();
			String tableNames = (String) request.getParameter("tableNames");
			String replyId = (String) request.getParameter("replyId");
			if (tableNames != null && !"".equalsIgnoreCase(tableNames) && !"".equalsIgnoreCase(tableNames)) {
				tablesArr = (JSONArray) JSONValue.parse(tableNames);
				if (tablesArr != null && !tablesArr.isEmpty()) {
					tableDiv = "<div id='userMergeTableColumnsRemoveDeleteId' class='userMergeTableColumnsRemoveDeleteClass'>"
							+ "<img src='images/delete_icon_hover.png' class='visionConversationalAiIcon' onclick='deleteFlowChartSelectedOperators()'/>"
							+ "</div>"
							+ "<div id='userMergeTableColumnsDivId' class='userTableColumnsDivClass text-right replyIntelisenseView noBubble'>";
					for (int i = 0; i < tablesArr.size(); i++) {
						String tableName = (String) tablesArr.get(i);
						String fetchQuery = "SELECT COLUMN_NAME  FROM USER_TAB_COLUMNS WHERE TABLE_NAME=:TABLE_NAME";
						Map mapData = new HashMap();
						mapData.put("TABLE_NAME", tableName);
						List listData = access.sqlqueryWithParams(fetchQuery, mapData);
						JSONObject mainInputObj = new JSONObject();
						JSONObject mainOutputObj = new JSONObject();
						if (listData != null && !listData.isEmpty()) {
							for (int j = 0; j < listData.size(); j++) {
								String columnName = (String) listData.get(j);
								JSONObject objData = new JSONObject();
								objData.put("label", columnName);
								// objData.put("multiple", true);
								if (i == 0) {
									mainOutputObj.put("output_" + j, objData);
								} else if (i == (tablesArr.size() - 1)) {
									mainInputObj.put("input_" + j, objData);
								} else {
									mainInputObj.put("input_" + j, objData);
									mainOutputObj.put("output_" + j, objData);
								}

							}
						}
						JSONObject putsObjData = new JSONObject();
						putsObjData.put("inputs", mainInputObj);
						putsObjData.put("outputs", mainOutputObj);
						tablesObj.put(tableName, putsObjData);
					}
					tableDiv += "</div>" + "<div class='userMergeTablesJoinErrorClass'>"
							+ "<input type='hidden' id='linkDynamicId' value='0'/>"
							+ "<div id='visionConvAIDefaultMapLinkColumnsId' class='visionConvAIDefaultMapLinkColumnsClass'></div>"
							+ "<button onclick='getMergeJoinCondColumns(" + replyId
							+ ")' class='userMergeTablesJoinErrorButtonClass'>Next</button>" + "</div>";
					resultObj.put("tableDiv", tableDiv);
					resultObj.put("tablesObj", tablesObj);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getVoiceSuggestedChartsBasedonColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			int colSize = 0;
			String colLength = request.getParameter("colLength");
			String colListStr = request.getParameter("columnsList");
			String axisColName = request.getParameter("axisColName");
			String title = request.getParameter("title");
			String dataTypeCountStr = request.getParameter("dataTypeCountObj");
			if (colLength != null && !"".equalsIgnoreCase(colLength)) {
				colSize = Integer.parseInt(colLength);
			}
			JSONObject dataTypesObj = new JSONObject();
			if (dataTypeCountStr != null && !"".equalsIgnoreCase(dataTypeCountStr)
					&& !"".equalsIgnoreCase(dataTypeCountStr)) {
				dataTypesObj = (JSONObject) JSONValue.parse(dataTypeCountStr);
			}
			long varCharCnt = 0;
			long numberCnt = 0;
			if (dataTypesObj != null && !dataTypesObj.isEmpty()) {
				varCharCnt = (long) dataTypesObj.get("VARCHAR2");
				numberCnt = (long) dataTypesObj.get("NUMBER");
			}
			if (colListStr != null && !"".equalsIgnoreCase(colListStr)) {
				colListStr = colListStr.replaceAll(" ", "#");
			}
			if (title != null && !"".equalsIgnoreCase(title)) {
				title = title.replaceAll(" ", "#");
			}
			String result = "<div id ='visionSuggestedChartTypes' class='visionSuggestedChartTypesClass'>"
					+ "<span class='visionSuggestionChartTypesSpan'>Please select the ChartType</span>"
					+ "<div id='visionSuggestionChartTypeId' class='visionSuggestionChartTypeClass row iconsRow'>";
			if (colSize == 1) {
				result += "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
						+ colListStr + "','indicator','" + axisColName + "','" + title
						+ "')  src='images/Guage.svg' class='visualDarkMode' title='Guage chart'></div>";
			} else if (colSize <= 2) {
				if (varCharCnt == 1 && numberCnt == 1) {
					result += "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','pie','" + axisColName + "','" + title
							+ "') src='images/Pie.svg' class='visualDarkMode' title='Pie chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','bar','" + axisColName + "','" + title
							+ "')  src='images/Bar.svg' class='visualDarkMode' title='Bar chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','donut','" + axisColName + "','" + title
							+ "')  src='images/Donut.svg' class='visualDarkMode' title='Donut chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','column','" + axisColName + "','" + title
							+ "')  src='images/Column.svg' class='visualDarkMode' title='Column chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','lines','" + axisColName + "','" + title
							+ "')  src='images/Line.svg' class='visualDarkMode' title='Line chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','scatter','" + axisColName + "','" + title
							+ "')  src='images/Scatter.svg' class='visualDarkMode' title='Scatter chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','histogram','" + axisColName + "','" + title
							+ "')  src='images/Histogram.svg' class='visualDarkMode' title='Histogram chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','funnel','" + axisColName + "','" + title
							+ "')  src='images/Funnel.svg' class='visualDarkMode' title='Funnel chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','waterfall','" + axisColName + "','" + title
							+ "')  src='images/Waterfall.svg' class='visualDarkMode' title='Waterfall chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','scatterpolar','" + axisColName + "','" + title
							+ "')  src='images/Redar-Chart.svg' class='visualDarkMode' title='Radar chart'></div>"

							/*
							 * +
							 * "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							 * + colListStr + "','barRotation','"+axisColName+"','"+
							 * title+"') src='images/Bar.svg' class='visualDarkMode' title='Bar Label Rotation chart'></div>"
							 */

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','treemap','" + axisColName + "','" + title
							+ "')  src='images/Tree_Chart.svg' class='visualDarkMode' title='Tree chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','BasicAreaChart','" + axisColName + "','" + title
							+ "') src='images/BasicAreaChart.png' class='visualDarkMode' title='Basic Area chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','AreaPiecesChart','" + axisColName + "','" + title
							+ "') src='images/AreaPiecesChart.png' class='visualDarkMode' title='Basic Area chart'></div>";

				}

			} else if (2 < colSize) {
				if (varCharCnt == 1 && numberCnt >= 1) {
					result += "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','bar','" + axisColName + "','" + title
							+ "')  src='images/Bar.svg' class='visualDarkMode' title='Bar chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','column','" + axisColName + "','" + title
							+ "')  src='images/Column.svg' class='visualDarkMode' title='Column chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','lines','" + axisColName + "','" + title
							+ "')  src='images/Line.svg' class='visualDarkMode' title='Line chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','scatter','" + axisColName + "','" + title
							+ "')  src='images/Scatter.svg' class='visualDarkMode' title='Scatter chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','histogram','" + axisColName + "','" + title
							+ "')  src='images/Histogram.svg' class='visualDarkMode' title='Histogram chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','funnel','" + axisColName + "','" + title
							+ "')  src='images/Funnel.svg' class='visualDarkMode' title='Funnel chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','candlestick','" + axisColName + "','" + title
							+ "')  src='images/Candlestick.svg' class='visualDarkMode' title='Candlestick chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','waterfall','" + axisColName + "','" + title
							+ "')  src='images/Waterfall.svg' class='visualDarkMode' title='Waterfall chart'></div>"
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','scatterpolar','" + axisColName + "','" + title
							+ "')  src='images/Redar-Chart.svg' class='visualDarkMode' title='Radar chart'></div>"

//							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= getSuggestedChartBasedonCols('"
//							+ colListStr + "','barRotation','" + tableName + "','" + joinQueryFlag + "','" + script
//							+ "','" + prependFlag + "') src='images/Bar.svg' class='visualDarkMode' title='Bar Label Rotation chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','StackedAreaChart','" + axisColName + "','" + title
							+ "') src='images/StackedAreaChart.png' class='visualDarkMode' title='Stacked Area Chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','GradStackAreaChart','" + axisColName + "','" + title
							+ "') src='images/GradientStackedAreaChart.png' class='visualDarkMode' title='Gradient Stacked Area chart'></div>";

				}
				if (varCharCnt >= 1 && numberCnt == 1) {
					result += "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','treemap','" + axisColName + "','" + title
							+ "')  src='images/Tree_Chart.svg' class='visualDarkMode' title='Tree chart'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','sunburst','" + axisColName + "','" + title
							+ "')  src='images/Sunburst_Inner_Icon.svg' class='visualDarkMode' title='SunBurst'></div>"

							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','sankey','" + axisColName + "','" + title
							+ "')  src='images/sankey_chart.png' class='visualDarkMode' title='Sankey'></div>";
				}
				if (varCharCnt == 2 && numberCnt == 1) {
					result += "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= viewChartBasedOnType('"
							+ colListStr + "','heatMap','" + axisColName + "','" + title
							+ "')  src='images/HeatMap_Inner_Icon.svg' class='visualDarkMode' title='Heat Map'></div>";

				}
			}

			result += "</div>" + "</div>";
			resultObj.put("result", result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}

	public JSONObject getInsightsView(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String tableName = (String) request.getParameter("tableName");
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> inputMap = new LinkedMultiValueMap<>();
			JSONObject dbDetails = new PilogUtilities().getDatabaseDetails(dataBaseDriver, dbURL, userName, password,
					"DH101102");
			inputMap.add("table_name", tableName);
			inputMap.add("USER_NAME", userName);
			inputMap.add("PASSWORD", password);
			inputMap.add("HOST", (String) dbDetails.get("HOST_NAME"));
			inputMap.add("PORT", (String) dbDetails.get("CONN_PORT"));
			inputMap.add("SERVICE_NAME", (String) dbDetails.get("CONN_DB_NAME"));
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(inputMap,
					headers);
			RestTemplate template = new RestTemplate();
			ResponseEntity<JSONObject> response = template
					.postForEntity("http://apihub.pilogcloud.com:6654/data_insights/", entity, JSONObject.class);
			JSONObject apiDataObj = response.getBody();
			if (apiDataObj != null && !apiDataObj.isEmpty()) {
				List insightsList = (List) apiDataObj.entrySet().stream().map(e -> ((Map.Entry) e).getKey())
						.collect(Collectors.toList());
				resultObj.put("insightList", insightsList);
			}
			resultObj.put("querysMap", apiDataObj);
			resultObj.put("tableName",tableName);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

//	@PersistenceContext
//	private EntityManager entityManager;
	@Transactional
	public JSONObject executeInsightsSQLQuery(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		try {
			String tableHeader = "";
			String query = request.getParameter("script");
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery(query);
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			ResultSetMetaData columns = results.getMetaData();

			//EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("YourPersistenceUnit");

			//EntityManager entityManager = entityManagerFactory.createEntityManager();
			//entityManager.getTransaction().begin();
//			Query query1 = entityManager.createQuery(query);
//			NativeQueryImpl nativeQuery = (NativeQueryImpl) query1;
//			nativeQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
//			List<Map<String,Object>> result = nativeQuery.getResultList();





			if (columnCount > 0) {
				tableHeader = "<thead>";
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					tableHeader += "<th>" + columnName + "</th>";
				}
				tableHeader += "</thead>";
			}

			String tableStr = "";
			if (query != null && !"".equalsIgnoreCase(query) && !"null".equalsIgnoreCase(query)) {
				List listData = access.sqlqueryWithParams(query, new HashMap());
				if (listData != null && !listData.isEmpty()) {
					tableStr = "<table id='visionInsightsChartDataTableId'>";
					tableStr += tableHeader;
					tableStr += "<tbody>";
					for (int i = 0; i < listData.size(); i++) {

						if (listData.get(i) instanceof BigDecimal) {
							tableStr += "<tr>";
							tableStr += "<td>" + listData.get(i) + "</td>";
							tableStr += "</tr>";
						} else {

							Object[] objData = (Object[]) listData.get(i);
							if (objData != null) {
								tableStr += "<tr>";
								for (int j = 0; j < objData.length; j++) {
									tableStr += "<td>" + objData[j] + "</td>";
								}
								tableStr += "</tr>";
							}
						}
					}
					tableStr += "</tbody>";
					tableStr += "</table>";
				}
			}
			resultObj.put("tableStr", tableStr);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return resultObj;
	}

    }
