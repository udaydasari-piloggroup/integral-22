/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pilog.mdm.DAO;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pilog.mdm.access.DataAccess;
import com.pilog.mdm.utilities.AuditIdGenerator;
import com.pilog.mdm.utilities.PilogUtilities;
import com.univocity.parsers.csv.CsvParserSettings;
import com.pilog.mdm.Utils.DashBoardUtills;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import java.util.Properties;
import com.sap.mw.jco.util.Codecs.Hex;
import static java.lang.Integer.parseInt;
import static net.sf.jsqlparser.parser.CCJSqlParserUtil.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


import oracle.sql.RAW;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.hibernate.SessionFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.opencsv.CSVReader;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
/**
 *
 * @author Jagadish.K
 */
@Repository
public class DashBoardsDAO {

	@Autowired
	public DataAccess access;
	@Value("${jdbc.driver}")
	private String dataBaseDriver; 
	@Value("${jdbc.username}")
	private String userName;
	@Value("${jdbc.password}")
	private String password;
	@Value("${jdbc.url}")
	private String dbURL;
//	@Value("${file.store.homedirectory}")
//	private String fileStoreHomedirectory;
	private PilogUtilities cloudUtills = new PilogUtilities();
	private Queue<Double> window = new LinkedList<>();
	private int period;
	private double sum;
	@Autowired
	public DashBoardUtills dashboardutils;
	
	@Autowired
    private IntelliSenseSheduleDAO cloudSheduleDAO;   

	private String fileStoreHomedirectory;
	private String fileSubStoreHomedirectory;

	// Initialization block which will be executed before constructor execution
	{
		if (System.getProperty("os.name").toUpperCase().startsWith("WINDOWS")) {
			fileStoreHomedirectory = "C:/Files/";
			fileSubStoreHomedirectory = "C:/";
		} else {
			fileStoreHomedirectory = "/u01/Files/";
			fileSubStoreHomedirectory = "/u01/";
		}
	}

	@Transactional
	public List getTreeListOpt(HttpServletRequest request, String treeId) {
		List<Object[]> treeList = new ArrayList();
		try {
			JSONObject labelObj = cloudUtills.getMultilingualObject(request);
			String query = "SELECT   TREE.TREE_ID,"// 0
					+ "         HIER.TREE_REF_TABLE,"// 1
					+ "         TREE.TREE_DESCR,"// 2
					+ "         TREE.THEME,"// 3
					+ "         TREE.WIDTH,"// 4
					+ "         TREE.HEIGHT,"// 5
					+ "         TREE.SELECTION_TYPE,"// 6
					+ "         TREE.ORGN_ID,"// 7
					+ "         TREE.ROOT_DESCR,"// 8
					+ "         HIER.ROLE_ID,"// 9
					+ "         HIER.TREE_PARAMS_ID,"// 10
					+ "         HIER.EDIT_FLAG,"// 11
					+ "         HIER.SEQUENCE_NO,"// 12
					+ "         HIER.HL_FLD_NAME,"// 13
					+ "         HIER.FLD_NAME,"// 14
					+ "         HIER.DISP_FLD_NAME,"// 15
					+ "         HIER.FOLLOWUP_COMP_ID,"// 16
					+ "         HIER.FOLLOWUP_COMP_TYPE,"// 17
					+ "         HIER.FOLLOWOP_COMP_DESCR,"// 18
					+ "         HIER.TREE_INIT_PARAMS,"// 19
					+ "         HIER.TREE_HIER_CUST_COL1,"// 20
					+ "         HIER.TREE_HIER_CUST_COL2,"// 21
					+ "         HIER.TREE_HIER_CUST_COL3,"// 22
					+ "         HIER.TREE_HIER_CUST_COL4,"// 23
					+ "         HIER.TREE_HIER_CUST_COL5,"// 24
					+ "         HIER.TREE_HIER_CUST_COL6,"// 25
					+ "         HIER.TREE_HIER_CUST_COL7,"// 26
					+ "         HIER.TREE_HIER_CUST_COL8,"// 27
					+ "         HIER.TREE_HIER_CUST_COL9,"// 28
					+ "         HIER.TREE_HIER_CUST_COL10,"// 29
					+ "         HIER.TREE_HIER_CUST_COL11,"// 30
					+ "         HIER.TREE_HIER_CUST_COL12,"// 31
					+ "         HIER.TREE_HIER_CUST_COL13,"// 32
					+ "         HIER.TREE_HIER_CUST_COL14,"// 33
					+ "         HIER.TREE_HIER_CUST_COL15"// 34
					+ "  FROM DAL_TREE TREE" + " INNER JOIN" + " DAL_TREE_ROLE_HIER HIER"
					+ " ON HIER.TREE_ID = TREE.TREE_ID" + " WHERE TREE.ORGN_ID = :ORGN_ID"
					+ " AND TREE.TREE_ID = :TREE_ID" + " AND HIER.ROLE_ID =:ROLE_ID ORDER BY HIER.SEQUENCE_NO";
			Map<String, Object> treeMap = new HashMap<>();
			treeMap.put("ORGN_ID", "C1F5CFB03F2E444DAE78ECCEAD80D27D");
			treeMap.put("ROLE_ID", "MM_MANAGER");
			treeMap.put("TREE_ID", treeId);
			System.out.println("query::" + query);
			System.out.println("treeMap:::" + treeMap);
			treeList = access.sqlqueryWithParams(query, treeMap);
			if (treeList != null && !treeList.isEmpty()) {
				treeList = treeList.stream().map(treeArray -> {
					treeArray[2] = cloudUtills.convertIntoMultilingualValue(labelObj, treeArray[2]);
					treeArray[8] = cloudUtills.convertIntoMultilingualValue(labelObj, treeArray[8]);
					return treeArray;
				}).collect(Collectors.toList());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return treeList;
	}

	@Transactional
	public String getLOV(HttpServletRequest request, String lovName) {
		String lovString = "";
		try {
			HttpSession httpSession = request.getSession(false);
			String ssOrgnId = (String) httpSession.getAttribute("ssOrgId");
			JSONObject labelsObj = cloudUtills.getMultilingualObject(request);
			List lovValuesList = getLOVList(request, lovName);
			if (lovValuesList != null && !lovValuesList.isEmpty()) {
				for (int i = 0; i < lovValuesList.size(); i++) {
					Object[] lovDataArray = (Object[]) lovValuesList.get(i);
					if (lovDataArray != null && lovDataArray[0] != null) {
						String selected = "";
						if (lovDataArray[3] != null && "SQL".equalsIgnoreCase(String.valueOf(lovDataArray[3]))) {
							String lovQuery = (String) lovDataArray[1];
							if (lovQuery != null && lovQuery.contains("<<--") && lovQuery.contains("-->>")) {
								lovQuery = replaceSessionValues(lovQuery, request);
							}
							System.out.println("lovQuery::::" + lovQuery);
							List dataList = access.sqlqueryWithParams(lovQuery, new HashMap<>());
							if (dataList != null && !dataList.isEmpty()) {
								for (int j = 0; j < dataList.size(); j++) {
									Object dataObject = dataList.get(j);
									if (dataObject instanceof Object[]) {
										Object[] dataObjectArray = (Object[]) dataList.get(j);
										lovString += "<option value='"
												+ (dataObjectArray[0] != null ? dataObjectArray[0] : "") + "' " + ">"
												+ cloudUtills.convertIntoMultilingualValue(labelsObj,
														dataObjectArray[1])
												+ "</option>";

									} else {
										lovString += "<option value='" + (dataObject != null ? dataObject : "") + "' "
												+ ">" + cloudUtills.convertIntoMultilingualValue(labelsObj, dataObject)
												+ "</option>";
									}

								}
							}

						} else {

							lovString += "<option data-optlabel='"
									+ cloudUtills.convertIntoMultilingualValue(labelsObj, lovDataArray[0]) + "' "
									+ "value='" + (lovDataArray[1] != null ? lovDataArray[1] : "") + "'" + " "
									+ selected + ">"
									+ cloudUtills.convertIntoMultilingualValue(labelsObj, lovDataArray[0])
									+ "</option>";

						}
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lovString;
	}

	@Transactional
	public List getLOVList(HttpServletRequest request, String lovName) {
		List lovValuesList = new ArrayList();
		try {
			HttpSession httpSession = request.getSession(false);
			String ssOrgnId = (String) httpSession.getAttribute("ssOrgId");

			JSONObject labelsObj = cloudUtills.getMultilingualObject(request);
//
			String lovQuery = " SELECT DISPLAY,PROCESS_VALUE,DEFAULT_FLAG,DATA_TYPE FROM DAL_DLOV WHERE DLOV_NAME =:DLOV_NAME AND ORGN_ID =:ORGN_ID ORDER BY SEQUENCE_NO";
			Map<String, Object> lovMap = new HashMap<>();
			lovMap.put("DLOV_NAME", lovName);
			lovMap.put("ORGN_ID", "C1F5CFB03F2E444DAE78ECCEAD80D27D");
			lovValuesList = access.sqlqueryWithParams(lovQuery, lovMap);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return lovValuesList;
	}

	@Transactional
	public String replaceSessionValues(String query, HttpServletRequest request) {
		try {

			if (query.contains("'<<--") && query.contains("-->>'")) {
				String sessionAtt = query.substring((query.indexOf("'<<--")) + 5, query.indexOf("-->>'"));
				String sessionval = ((String) request.getSession(false).getAttribute(sessionAtt)) != null
						? (String) request.getSession(false).getAttribute(sessionAtt)
						: "";
				String replaceval = "'<<--" + sessionAtt + "-->>'";
				String query1 = query.substring(0, (query.indexOf("'<<--")));
				String query2 = query.substring((query.indexOf("-->>'")) + 5);
				query = query1 + "'" + sessionval + "'" + query2;
			}
			if (query.contains("<<--") && query.contains("-->>")
					&& !(query.contains("'<<--") && query.contains("-->>'"))) {
				String sessionAtt = query.substring((query.indexOf("<<--")) + 4, query.indexOf("-->>"));
				String sessionval = ((String) request.getSession(false).getAttribute(sessionAtt)) != null
						? (String) request.getSession(false).getAttribute(sessionAtt)
						: "";
				String replaceval = "<<--" + sessionAtt + "-->>";
				String query1 = query.substring(0, (query.indexOf("<<--")));
				String query2 = query.substring((query.indexOf("-->>")) + 4);
				query = query1 + "'" + sessionval + "'" + query2;
			}
			if (query.contains("'<<-") && query.contains("->>'")) {
				String sessionAtt = query.substring((query.indexOf("'<<-")) + 4, query.indexOf("->>'"));
				String sessionval = ((String) request.getSession(false).getAttribute(sessionAtt)) != null
						? (String) request.getSession(false).getAttribute(sessionAtt)
						: "";
				String replaceval = "'<<-" + sessionAtt + "->>'";
				String query1 = query.substring(0, (query.indexOf("'<<-")));
				String query2 = query.substring((query.indexOf("->>'")) + 4);
				query = query1 + "'" + sessionval + "'" + query2;
			}
			if (query.contains("<<-") && query.contains("->>") && !(query.contains("'<<-") && query.contains("->>'"))
					&& !(query.contains("<<--") && query.contains("-->>"))
					&& !(query.contains("'<<--") && query.contains("-->>'"))) {
				String sessionAtt = query.substring((query.indexOf("<<-")) + 3, query.indexOf("->>"));
				String sessionval = ((String) request.getSession(false).getAttribute(sessionAtt)) != null
						? (String) request.getSession(false).getAttribute(sessionAtt)
						: "";
				String replaceval = "<<-" + sessionAtt + "->>";
				String query1 = query.substring(0, (query.indexOf("<<-")));
				String query2 = query.substring((query.indexOf("->>")) + 3);
				query = query1 + "'" + sessionval + "'" + query2;
			}

			if ((query.contains("<<--") && query.contains("-->>"))
					|| (query.contains("<<-") && query.contains("->>"))) {
				query = replaceSessionValues(query, request);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query;
	}

	@Transactional
	public List getLOVListData(HttpServletRequest request, String lovName) {
		List lovValuesList = new ArrayList();
		try {
			HttpSession httpSession = request.getSession(false);
			String ssOrgnId = (String) httpSession.getAttribute("ssOrgId");
			String lovQuery = " SELECT DISPLAY FROM DAL_DLOV WHERE DLOV_NAME =:DLOV_NAME AND ORGN_ID =:ORGN_ID ORDER BY SEQUENCE_NO";
			Map<String, Object> lovMap = new HashMap<>();
			lovMap.put("DLOV_NAME", lovName);
			lovMap.put("ORGN_ID", ssOrgnId);
			lovValuesList = access.sqlqueryWithParams(lovQuery, lovMap);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return lovValuesList;
	}

	@Transactional
	public void saveUserFiles(HttpServletRequest request, String orginalFileName, String fileName, String filePath,
			String fileType) {
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = DriverManager.getConnection(dbURL, userName, password);
			String insertQuery = "INSERT INTO DAL_DM_SAVED_FILES(ORGN_ID, USER_NAME, "
					+ "FILE_ORG_NAME, FILE_NAME, FILE_PATH, FILE_TYPE, CREATE_BY, EDIT_BY,FILE_CONTENT)"
					+ " VALUES(?,?,?,?,?,?,?,?,?)";
			statement = connection.prepareStatement(insertQuery);

			statement.setObject(1, request.getSession(false).getAttribute("ssOrgId"));// ORGN_ID
			statement.setObject(2, request.getSession(false).getAttribute("ssUsername"));// USER_NAME
			statement.setObject(3, orginalFileName);// FILE_ORG_NAME
			statement.setObject(4, fileName);// FILE_NAME
			statement.setObject(5, filePath);// FILE_PATH
			statement.setObject(6, fileType);// FILE_TYPE
			statement.setObject(7, request.getSession(false).getAttribute("ssUsername"));// CREATE_BY
			statement.setObject(8, request.getSession(false).getAttribute("ssUsername"));// EDIT_BY
			File folderPath = new File(filePath);
			if (!folderPath.exists()) {
				folderPath.mkdirs();
			}
			File toBeSaveFile = new File(folderPath.getAbsolutePath() + File.separator + fileName);
			FileInputStream fis = new FileInputStream(toBeSaveFile);
			statement.setBinaryStream(9, fis, (int) toBeSaveFile.length());// FILE_CONTENT
			System.out.println("insertQuery:::" + insertQuery);
			int insertCount = statement.executeUpdate();
			System.out.println("insertCount::" + insertCount);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {
			}
		}
	}

	public String getLoadTableColumns(HttpServletRequest request) {
		String result = "";
		Connection connection = null;
		try {
			String tableName = request.getParameter("tableName");
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				result = "<div  class='visionVisualizeChartTableToggleClass'>";
//                result = "<div id='" + tableName + "_ID' class='visionVisualizeChartTableToggleClass'>";
				result += "<div class='visionVisualizeChartTableClass'><img src=\"images/nextrightarrow.png\" id=\"VisionImageVisualizationTableId\" class=\"VisionImageVisualizationTableClass\" title=\"Show/Hide Table\"/>"
						+ "<img src=\"images/GridDB.png\" class=\"VisionImageVisualizationTableImageClass\" title=\"Show/Hide Table\"/><h6>"
						+ tableName + "</h6></div>";
				result += "<ul class='visionVisualizationDragColumns'>";
				result += "<div class='columnFilterDiv'><input type='text' id='name' class='columnFilterationClass' placeholder='Search Column'></div>";
				result += "<div class='tableColumnsList'>";
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					String id = tableName + "_" + columnName;
					if (columnType != null && !"".equalsIgnoreCase(columnType)
							&& "NUMBER".equalsIgnoreCase(columnType)) {
						result += "<li id=\"" + id
								+ "\" ><img src=\"images/sigma-icon.jpg\" class=\"VisionImageVisualizationTableClass\"/>"
								+ columnName + "</li>";
					} else if (columnType != null && !"".equalsIgnoreCase(columnType)
							&& "DATE".equalsIgnoreCase(columnType)) {
						result += "<li id=\"" + id
								+ "\" ><img src=\"images/calendar.svg\" class=\"VisionImageVisualizationTableClass\"/>"
								+ columnName + "</li>";
					} else {
						result += "<li id=\"" + id + "\" >" + columnName + "</li>";
					}
				}
				result += "<li style='display:none'><span>No Columns Found</span></li>";
				result += "</div></ul>";
				result += "</div>";
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	@Transactional
	public JSONObject fetchChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		JSONObject filteredChartConfigObj = new JSONObject();
		try {
			List selectData = null;
			List<String> columnKeys = new ArrayList<>();
			JSONObject chartConfigObj = new JSONObject();
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			String colorsObj= request.getParameter("colorsObj");
			//String chartCOnfigObjStr = request.getParameter("chartCOnfigObjStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			System.out.println("LayoutObj :::" + layoutObj);
			System.out.println("DataPropObj :::" + dataPropObj);
			String compareChartsFlag = request.getParameter("compareChartsFlag");
			JSONObject chartListObj = new JSONObject();
			if (compareChartsFlag != null && !"".equalsIgnoreCase(compareChartsFlag)
					&& ("Y".equalsIgnoreCase(compareChartsFlag) || "YF".equalsIgnoreCase(compareChartsFlag))) {
				chartListObj = getCompareChartDataList(request);
			} else {
				chartListObj = getChartDataList(request);
			}
			int totalChartCount = 0;
			if (chartListObj != null && !chartListObj.isEmpty()) {
				selectData = (List) chartListObj.get("chartList");
				columnKeys = (List<String>) chartListObj.get("columnKeys");
				if (chartListObj.get("totalChartCount") != null
						&& !"".equalsIgnoreCase(String.valueOf(chartListObj.get("totalChartCount")))) {
					totalChartCount = ((BigDecimal) chartListObj.get("totalChartCount")).intValue();
					chartObj.put("totalChartCount", totalChartCount);
				}

			}
			JSONObject framedChartDataObj = getFramedChartDataObject(request, selectData, columnKeys, layoutObj,
					dataPropObj, chartType);
			if (framedChartDataObj != null && !framedChartDataObj.isEmpty()) {
				chartObj.put("layout", (JSONObject) framedChartDataObj.get("layoutObj"));
				if (chartType != null && !"".equalsIgnoreCase(chartType) && "treemap".equalsIgnoreCase(chartType)) {
					JSONObject treeMapDataObj = getTreeMapDataObject(framedChartDataObj, columnKeys);
					if (treeMapDataObj != null && !treeMapDataObj.isEmpty()) {
						chartObj.put("treeMapCol", treeMapDataObj.get("treeMapColObj"));
						chartObj.put("data", treeMapDataObj.get("data"));
					}
				} else if (chartType != null && !"".equalsIgnoreCase(chartType)
						&& "indicator".equalsIgnoreCase(chartType)) {
					JSONObject indicatorObj = getIndicatorDataObject(framedChartDataObj, columnKeys);
					if (indicatorObj != null && !indicatorObj.isEmpty()) {
						chartObj.put("data", indicatorObj.get("data"));
						chartObj.put("gauge", indicatorObj.get("gauge"));
					}
				} else if (chartType != null && !"".equalsIgnoreCase(chartType)
						&& "barRotation".equalsIgnoreCase(chartType)) {
					JSONObject barRotationDataObj = getBarRotationDataObject(
							(JSONObject) framedChartDataObj.get("dataObj"), columnKeys);
					if (barRotationDataObj != null && !barRotationDataObj.isEmpty()) {
						chartObj.put("xAxis", barRotationDataObj.get("xAxis"));
						chartObj.put("yAxis", barRotationDataObj.get("yAxis"));
						chartObj.put("series", barRotationDataObj.get("series"));
						chartObj.put("legend", barRotationDataObj.get("legend"));
					}
				} else {
					chartObj.put("data", (JSONObject) framedChartDataObj.get("dataObj"));
				}
			}
			chartObj.put("dataPropObject", dataPropObj);
			chartObj.put("compareChartFlag", compareChartsFlag);
			chartObj.put("colorsObj",colorsObj);
			chartObj.put("chartCOnfigObjStr", chartConfigObjStr);

//            insertChartDetailsInTable(dataPropObj, dataObj, layoutObj, chartId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public JSONObject getTreeMapDataObject(JSONObject framedDataObject, List columnKeys) {
		JSONObject treeDataMapObj = new JSONObject();
		try {
			long treeMapVal = 0;
			JSONObject dataObj = (JSONObject) framedDataObject.get("dataObj");
			if (dataObj != null && !dataObj.isEmpty()) {
				if (columnKeys != null && !columnKeys.isEmpty() && columnKeys.size() > 2) {
					JSONObject treeMapCol = new JSONObject();
					treeMapCol.put("parents", columnKeys.get(0));
					treeMapCol.put("labels", columnKeys.get(1));
					treeMapCol.put("values", columnKeys.get(2));
					JSONArray treeMapParents = (JSONArray) dataObj.get((String) columnKeys.get(0));
					JSONArray treeMapLabels = (JSONArray) dataObj.get((String) columnKeys.get(1));
					JSONArray treeMapValues = (JSONArray) dataObj.get((String) columnKeys.get(2));
					if (treeMapParents != null && !treeMapParents.isEmpty() && treeMapValues != null
							&& !treeMapValues.isEmpty()) {
						JSONObject treeDataObj = new JSONObject();
						for (int i = 0; i < treeMapParents.size(); i++) {
							if (treeDataObj != null && !treeDataObj.isEmpty()
									&& treeDataObj.containsKey(treeMapParents.get(i))) {
								JSONArray totalArr = (JSONArray) treeDataObj.get(treeMapParents.get(i));
								if (totalArr != null && !totalArr.isEmpty()) {
									JSONArray treeMapParentArr = (JSONArray) totalArr.get(0);
									JSONArray treeMapLabelArr = (JSONArray) totalArr.get(1);
									JSONArray treeMapValueArr = (JSONArray) totalArr.get(2);
									treeMapParentArr.add(treeMapParents.get(i));
									treeMapLabelArr.add(treeMapLabels.get(i));
									treeMapValueArr.add(treeMapValues.get(i));
									JSONArray treeMapArr = new JSONArray();
									treeMapArr.add(treeMapParentArr);
									treeMapArr.add(treeMapLabelArr);
									treeMapArr.add(treeMapValueArr);
									treeDataObj.put(treeMapParents.get(i), treeMapArr);
								}
							} else {
								JSONArray totalArr = new JSONArray();
								JSONArray treeMapParentArr = new JSONArray();
								JSONArray treeMapLabelArr = new JSONArray();
								JSONArray treeMapValueArr = new JSONArray();
								treeMapParentArr.add(treeMapParents.get(i));
								treeMapLabelArr.add(treeMapLabels.get(i));
								treeMapValueArr.add(treeMapValues.get(i));
								totalArr.add(treeMapParentArr);
								totalArr.add(treeMapLabelArr);
								totalArr.add(treeMapValueArr);
								treeDataObj.put(treeMapParents.get(i), totalArr);
							}
						}
						JSONObject data = new JSONObject();
						for (Object key : treeDataObj.keySet()) {
							String keyVal = (String) key;
							JSONArray totalArr = (JSONArray) treeDataObj.get(keyVal);
							if (totalArr != null && !totalArr.isEmpty()) {
								JSONArray treeMapParentArr = (JSONArray) totalArr.get(0);
								JSONArray treeMapLabelArr = (JSONArray) totalArr.get(1);
								JSONArray treeMapValueArr = (JSONArray) totalArr.get(2);
								long longVal = 0;
								for (int k = 0; k < treeMapValueArr.size(); k++) {
									BigDecimal decVal = (BigDecimal) treeMapValueArr.get(k);
									if (decVal != null) {
										longVal += decVal.longValue();
									}
								}
								treeMapParentArr.add(0, "");
								treeMapLabelArr.add(0, keyVal);
								treeMapValueArr.add(0, longVal);
								dataObj = new JSONObject();
								dataObj.put(columnKeys.get(0), treeMapParentArr);
								dataObj.put(columnKeys.get(1), treeMapLabelArr);
								dataObj.put(columnKeys.get(2), treeMapValueArr);
								data.put(keyVal, dataObj);
							}

						}
						treeDataMapObj.put("data", data);
						treeDataMapObj.put("treeMapColObj", treeMapCol);
					}
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return treeDataMapObj;
	}

	@Transactional
	public JSONObject getFramedChartDataObject(HttpServletRequest request, List selectData, List<String> columnKeys,
			JSONObject layoutObj, JSONObject dataPropObj, String chartType) {

		Double currencyConversionRate = null;
		JSONArray colorsArr = new JSONArray();
		JSONArray markerColorsArr = new JSONArray();
		JSONObject dataObj = new JSONObject();
		JSONObject framedChartDataObj = new JSONObject();
		if (dataPropObj != null && !dataPropObj.isEmpty()) {
			JSONObject markerObj = (JSONObject) dataPropObj.get("marker");
			if (markerObj != null && !markerObj.isEmpty()) {
				if (markerObj.get("colors") instanceof JSONArray) {
					colorsArr = (JSONArray) markerObj.get("colors");
				} else {
					String colorValues = (String) markerObj.get("colors");
					if (colorValues != null && !"".equalsIgnoreCase(colorValues)
							&& !"null".equalsIgnoreCase(colorValues)) {
						colorsArr.add(colorValues);
					}
				}

			}
		}
		if (selectData != null && !selectData.isEmpty()) {
			int c = 0;
			if (chartType != null && !"".equalsIgnoreCase(chartType) && "indicator".equalsIgnoreCase(chartType)) {
				long indicatorVal = 0;
				for (int i = 0; i < selectData.size(); i++) {
					if (selectData.get(i) instanceof String) {
						String rowData = (String) selectData.get(i);
						if (rowData != null && !"".equalsIgnoreCase(rowData)) {
							indicatorVal += Integer.parseInt(rowData);
						}
					} else if (selectData.get(i) instanceof Timestamp) {
						String rowData = (String) selectData.get(i);
						if (rowData != null && !"".equalsIgnoreCase(rowData)) {
							indicatorVal += Integer.parseInt(rowData);
						}
					} else if (selectData.get(i) instanceof BigDecimal) {
						BigDecimal rowData = (BigDecimal) selectData.get(i);
						if (rowData != null) {
							indicatorVal = rowData.longValue();
						}
					}

					if (colorsArr != null && !colorsArr.isEmpty()) {
						if (c > colorsArr.size() - 1) {
							c = 0;
						}
						markerColorsArr.add(colorsArr.get(c));
					}
					c++;
				}
				dataObj.put(columnKeys.get(0), indicatorVal);

			} else {
				String currencyConversionEvent = request.getParameter("isCurrencyConversionEvent");
				boolean isCurrencyConversionEvent = false;
				if (currencyConversionEvent != null && !"".equalsIgnoreCase(currencyConversionEvent)
						&& !"null".equalsIgnoreCase(currencyConversionEvent)) {
					isCurrencyConversionEvent = Boolean.parseBoolean(currencyConversionEvent);
					currencyConversionRate = getCurrencyConversionRate(request);
				}
				for (int i = 0; i < selectData.size(); i++) {
					Object[] rowData = (Object[]) selectData.get(i);
					for (int j = 0; j < rowData.length; j++) {
						if (dataObj != null && !dataObj.isEmpty() && dataObj.get(columnKeys.get(j)) != null) {
							JSONArray jsonDataArr = (JSONArray) dataObj.get(columnKeys.get(j));
							if (rowData[j] != null) {
								if (j >= 1 && currencyConversionRate != null && isCurrencyConversionEvent) {
									double chartValueIndouble = (Double) dashboardutils
											.getRequiredObjectTypeFromObject(rowData[j], "BigDecimal", "Double");
									double convertedCurrencyValue = chartValueIndouble * currencyConversionRate;
									jsonDataArr.add(convertedCurrencyValue);
								} else {
									if (rowData[j] instanceof Timestamp) {
										Timestamp ts = (Timestamp) rowData[j];
										Date date = new Date(ts.getTime());
										SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
										String dateStr = sdf.format(date);
										jsonDataArr.add(dateStr);
									} else {
										jsonDataArr.add(rowData[j]);
									}

								}

							} else {
								if (j >= 1) {
									jsonDataArr.add(0);
								}

							}
							dataObj.put(columnKeys.get(j), jsonDataArr);
						} else {
							JSONArray jsonDataArr = new JSONArray();
							if (rowData[j] != null) {
								if (j >= 1 && currencyConversionRate != null && isCurrencyConversionEvent) {
									double chartValueIndouble = (Double) dashboardutils
											.getRequiredObjectTypeFromObject(rowData[j], "BigDecimal", "Double");
									double convertedCurrencyValue = chartValueIndouble * currencyConversionRate;
									jsonDataArr.add(convertedCurrencyValue);
								} else {
									if (rowData[j] instanceof Timestamp) {
										Timestamp ts = (Timestamp) rowData[j];
										Date date = new Date(ts.getTime());
										SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
										String dateStr = sdf.format(date);
										jsonDataArr.add(dateStr);
									} else {
										jsonDataArr.add(rowData[j]);
									}
								}
							} else {
								if (j >= 1) {
									jsonDataArr.add(0);
								}
							}
							dataObj.put(columnKeys.get(j), jsonDataArr);
						}
					}

					if (colorsArr != null && !colorsArr.isEmpty()) {
						if (c > colorsArr.size() - 1) {
							c = 0;
						}
						markerColorsArr.add(colorsArr.get(c));
					}
					c++;
				}
			}
			framedChartDataObj.put("dataObj", dataObj);
		}

		if (layoutObj != null && !layoutObj.isEmpty()) {
			JSONObject markerObj = (JSONObject) layoutObj.get("marker");
			if (markerObj != null && !markerObj.isEmpty() && markerColorsArr != null && !markerColorsArr.isEmpty()) {
				markerObj.put("colors", markerColorsArr);
			}
			framedChartDataObj.put("layoutObj", layoutObj);
		}

		return framedChartDataObj;
	}

	public JSONObject getChartDataList(HttpServletRequest request) {
		JSONObject chartListObj = new JSONObject();
		try {
			boolean flag = false;
			String selectQuery = "";
			String whereCondQuery = "";
			String groupByCond = "";
			String uniqueCountGroupByCond = "";
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String chartPorpObj = request.getParameter("chartPorpObj");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String startIndex = request.getParameter("startIndex");
			String endIndex = request.getParameter("endIndex");
			String pageSize = request.getParameter("pageSize");
			String orderBy = "";
			String uniqueCountOrderBy = "";
			int limit = 0;
			int startLimit = 0;
			if (startIndex != null && !"".equalsIgnoreCase(startIndex) && endIndex != null
					&& !"".equalsIgnoreCase(endIndex)) {
				limit = (int) Integer.parseInt(endIndex);
				startLimit = Integer.parseInt(startIndex);
			}
			List<String> columnKeys = new ArrayList<>();
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			String uniqueCountQuery = "";
			String uniqueQueryFlag = "false";
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							String[] columns = columnName.split(",");
							if (columns != null && columns.length > 0) {
								for (int j = 0; j < columns.length; j++) {
									String column = columns[j];
									String[] filteredColumnnameArr = column.split("\\.");
									String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
									if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
											&& !"null".equalsIgnoreCase(filteredColumnname)) {
										uniqueCountQuery += " " + filteredColumnname + ", ";
										filteredColumnname = filteredColumnname.replaceAll("_", " ");
									}
									columnKeys.add(filteredColumnname);
									selectQuery += " " + column + ", ";
									groupByCond += column + ", ";
									uniqueCountGroupByCond += filteredColumnnameArr[1] + ", ";
									whereCondQuery += column + " IS NOT NULL ";
									if (j == 0 && "waterfall".equalsIgnoreCase(chartType)) {
										orderBy += " ORDER BY " + column + " ASC ";
									}
									if (i != axisColsArr.size() - 1) {
										whereCondQuery += " AND ";
									}

								}
							}
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						String[] filteredColumnnameArr = columnName.split("\\.");
						String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
						if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
								&& !"null".equalsIgnoreCase(filteredColumnname)) {
							filteredColumnname = filteredColumnname.replaceAll("_", " ");
						}
						columnKeys.add(filteredColumnname + "ASCOL" + i);
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {

							if (aggColumnName.equalsIgnoreCase("UniqueCount")) {
								if (uniqueQueryFlag != null && "false".equalsIgnoreCase(uniqueQueryFlag)) {
									uniqueCountOrderBy += " ORDER BY COUNT(COL" + i + ") DESC ";
								}
								String[] ColumnNameArr = columnName.split("\\(");
								columnName = ColumnNameArr[1].replaceAll("\\)", "");
								uniqueCountQuery += " COUNT(COL" + i + ") ,";
								selectQuery += " " + columnName + " AS COL" + i + " ,";
								uniqueQueryFlag = "true";
							} else {
								selectQuery += " " + columnName + " AS COL" + i + " ,";
							}
							if (i == 0 && !"waterfall".equalsIgnoreCase(chartType)) {
								orderBy += " ORDER BY COL" + i + " DESC ";
							}
							flag = true;
						} else {
							selectQuery += " " + columnName + ", ";
							if (i == 0 && !"waterfall".equalsIgnoreCase(chartType)) {
								orderBy += " ORDER BY " + columnName + " DESC ";
							}
							groupByCond += columnName;
							uniqueCountGroupByCond += filteredColumnnameArr[1] + ", ";
							if (i < valuesColsArr.size() - 1) {
								groupByCond += ",";
								uniqueCountGroupByCond += ",";
							}
						}
					}
				}

				if (chartType != null && !"".equalsIgnoreCase(chartType)
						&& ("indicator".equalsIgnoreCase(chartType) || "Card".equalsIgnoreCase(chartType))) {
					groupByCond = "";
				} else if (!flag) {
					groupByCond = "";
				} else if (groupByCond != null && !"".equalsIgnoreCase(groupByCond)) {
					groupByCond = new PilogUtilities().trimChar(groupByCond, ',');
					groupByCond = " GROUP BY " + groupByCond;
					uniqueCountGroupByCond = new PilogUtilities().trimChar(uniqueCountGroupByCond, ',');
					uniqueCountGroupByCond = " GROUP BY " + uniqueCountGroupByCond;
				}

			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);

					if (filterColObj.get("values") instanceof JSONArray) {
						JSONArray valuesArr = (JSONArray) filterColObj.get("values");
						if (valuesArr != null && !valuesArr.isEmpty()) {
							@SuppressWarnings("unchecked")
							String values = (String) valuesArr.stream().map(e -> e).collect(Collectors.joining(","));
							filterColObj.put("values", values);
						}
					}

					if (filterColObj != null && !filterColObj.isEmpty()) {
						if (i == 0 && whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)) {
							whereCondQuery += " AND ";
						}
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			if (selectQuery != null && !"".equalsIgnoreCase(selectQuery) && tablesArr != null && !tablesArr.isEmpty()) {

				String tableName = (String) tablesArr.get(0);
				String countQuery = "";
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
					selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
					uniqueCountQuery = new PilogUtilities().trimChar(uniqueCountQuery, ',');
					if (uniqueQueryFlag != null && !"".equalsIgnoreCase(uniqueQueryFlag)
							&& "true".equalsIgnoreCase(uniqueQueryFlag)) {
						selectQuery = "SELECT " + selectQuery + " " + tableName + whereCondQuery + orderBy;
						selectQuery = "SELECT " + uniqueCountQuery + " FROM (" + selectQuery + ") "
								+ uniqueCountGroupByCond + uniqueCountOrderBy;
					} else {
						selectQuery = "SELECT " + selectQuery + " " + tableName + whereCondQuery + groupByCond
								+ orderBy;
					}
					countQuery = "SELECT COUNT(*) " + tableName + whereCondQuery + groupByCond;
					System.out.println("selectQuery :::" + selectQuery);
				} else {
					selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
					uniqueCountQuery = new PilogUtilities().trimChar(uniqueCountQuery, ',');
					if (uniqueQueryFlag != null && !"".equalsIgnoreCase(uniqueQueryFlag)
							&& "true".equalsIgnoreCase(uniqueQueryFlag)) {
						selectQuery = "SELECT " + selectQuery + " FROM " + tableName + whereCondQuery + orderBy;
						selectQuery = "SELECT " + uniqueCountQuery + " FROM (" + selectQuery + ") "
								+ uniqueCountGroupByCond + uniqueCountOrderBy;
					} else {
						selectQuery = "SELECT " + selectQuery + " FROM " + tableName + whereCondQuery + groupByCond
								+ orderBy;
					}
					countQuery = "SELECT COUNT(*) FROM " + tableName + whereCondQuery + groupByCond;
					System.out.println("selectQuery :::" + selectQuery);
				}

				if (countQuery != null && !"".equalsIgnoreCase(countQuery)) {
					countQuery = countQuery.replace("#", " "); // Replace # with space
					 if(countQuery.contains("$")) {
	            	countQuery = countQuery.replace("$", ",");
					 }
					List countData = access.sqlqueryWithParams(countQuery, new HashMap());
					if (countData != null && !countData.isEmpty()) {
						chartListObj.put("totalChartCount", countData.get(0));
					}
				}

			}
			selectQuery = selectQuery.replaceAll("#", " ");
			 if(selectQuery.contains("$")) {
			 selectQuery = selectQuery.replaceAll("$", ",");
			 }
			List selectData = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), limit, 0);
			if (selectData != null && !selectData.isEmpty()) {
				chartListObj.put("chartList", selectData);
			}
			if (columnKeys != null && !columnKeys.isEmpty()) {
				chartListObj.put("columnKeys", columnKeys);
			}
			System.out.println("selectQuery :::" + selectQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chartListObj;
	}

	@Transactional
	public JSONObject getSaveFilterColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			JSONArray resultStrArr = new JSONArray();
			JSONArray resultListArr = new JSONArray();
			JSONArray resultValuesArr = new JSONArray();
			JSONArray resultOpartorArr = new JSONArray();
			String chartId = request.getParameter("id");
			String selectQuery = "SELECT FILTER_CONDITION FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID AND ROLE_ID =:ROLE_ID AND ORGN_ID =:ORGN_ID";
			Map selectMap = new HashMap();
			selectMap.put("CHART_ID", chartId);
			selectMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
			selectMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
			List listData = access.sqlqueryWithParams(selectQuery, selectMap);
			if (listData != null && !listData.isEmpty()) {
				String filterStr = (String) listData.get(0);
				if (filterStr != null && !"".equalsIgnoreCase(filterStr)) {
					JSONArray filterArr = (JSONArray) JSONValue.parse(filterStr);
					if (filterArr != null && !filterArr.isEmpty()) {
						int j = 100;
						for (int i = 0; i < filterArr.size(); i++) {
							JSONObject filterObj = (JSONObject) filterArr.get(i);
							if (filterObj != null && !filterObj.isEmpty()) {
								String tableName = "";
								String colName = (String) filterObj.get("colName");
								String operator = (String) filterObj.get("operator");
								String values = (String) filterObj.get("values");
								if (colName != null && !"".equalsIgnoreCase(colName)) {
									tableName = colName.split("\\.")[0];
									colName = colName.split("\\.")[1];
								}
								JSONObject responseObj = fetchSavedFiltersValues(request, colName, tableName, operator,
										values, j);
								if (responseObj != null && !responseObj.isEmpty()) {
									String result = (String) responseObj.get(colName);
									JSONArray resultArr = (JSONArray) responseObj.get(colName + "checkBoxList");
									resultStrArr.add(result);
									resultListArr.add(resultArr);
									resultValuesArr.add(values);
									resultOpartorArr.add(operator);
								}
								j++;
							}
						}
					}
				}
			}
			resultObj.put("resultStrArr", resultStrArr);
			resultObj.put("resultListArr", resultListArr);
			resultObj.put("resultValuesArr", resultValuesArr);
			resultObj.put("resultOpartorArr", resultOpartorArr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject fetchSavedFiltersValues(HttpServletRequest request, String columnName, String tableName,
			String operator, String values, int filterCnt) {
		JSONObject resultObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			String result = "";
			int cnt = filterCnt;
			JSONArray checkBoxDataArr = new JSONArray();
			String selectQuery = "";
			String operators = "<select id ='visionVisualizeChartFiltersFieldOperatorsId" + filterCnt
					+ "' class='visionVisualizeChartFiltersOperatorsClass'>" + "<option value= 'IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "In") + "</option>"
					+ "<option value= 'Containing'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Containing") + "</option>"
					+ "<option value= 'EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Equals") + "</option>"
					+ "<option value= 'LIKE'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Like")
					+ "</option>" + "<option value= 'BEGINING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Beginning With") + "</option>"
					+ "<option value= 'ENDING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Ending With") + "</option>"
					+ "<option value= 'NOT EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Equals") + "</option>"
					+ "<option value= 'NOT IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not In") + "</option>"
					+ "<option value= 'IS'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is")
					+ "</option>" + "<option value= 'IS NOT'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is Not") + "</option>"
					+ "<option value= 'NOT LIKE'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Like") + "</option>"
					+ "<option value= 'BETWEEN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Between") + "</option>"
					+ "</select>";
			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				tableName = tableName.replace("_ID", "");
				selectQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName;
				List selectData = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), 50, 0);
				if (selectData != null && !selectData.isEmpty()) {
					result = "<div id ='visionVisualizeChartFiltersFieldDivId" + filterCnt
							+ "' class='visionVisualizeChartFiltersFieldDivClass'>"
							+ "<div class='visionVisualizeChartFiltersFieldOperator'> <div id ='visionVisualizeChartFiltersFieldId"
							+ filterCnt + "' class='visionVisualizeChartFiltersFieldsClass'>"
							+ "<input type='hidden' id='visionVisualizeChartFiltersHiddenName" + filterCnt + "' value='"
							+ tableName + "." + columnName + "'/><span class='visionVisualizeChartFiltersFieldSpan'>"
							+ columnName
							+ "</span><img src='images/close_white.png' title=\"Remove Column\" onclick=\"RemoveFilterColumns('"
							+ filterCnt + "','','" + cnt + "')\"/></div>"
							+ "<div id ='visionVisualizeChartFiltersFieldOperatorsDivId" + filterCnt
							+ "' class='visionVisualizeChartFiltersFieldOperatorsClass'>" + operators + "</div></div>"
							+ "<div id ='visionVisualizeChartFiltersFieldValuesId" + filterCnt
							+ "' class='visionVisualizeChartFiltersFieldValuesClass' >";
					for (int i = 0; i < selectData.size(); i++) {
						String checkBoxValue = "";
						if (selectData.get(i) instanceof String) {
							checkBoxValue = (String) selectData.get(i);
							if (checkBoxValue != null && !"".equalsIgnoreCase(checkBoxValue)
									&& !"null".equalsIgnoreCase(checkBoxValue)) {
								checkBoxValue = checkBoxValue.trim();
							}
						}
						JSONObject checkBoxData = new JSONObject();
						checkBoxData.put("text", checkBoxValue);
						checkBoxData.put("value", checkBoxValue);
						checkBoxDataArr.add(checkBoxData);

					}
					result += "</div>" + "</div>";
				}
			}
			resultObj.put(columnName, result);
			resultObj.put(columnName + "checkBoxList", checkBoxDataArr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject fetchFiltersValues(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONArray checkBoxDataArr = new JSONArray();
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			int cnt = 0;
			int filterCnt = 0;
			String result = "";
			String selectQuery = "";
			String count = request.getParameter("count");
			String tableName = request.getParameter("id");
			String columnName = request.getParameter("label");
			String divid = request.getParameter("divid");
			String chartType = request.getParameter("chartType");
			String filterCount = request.getParameter("filterCount");
			String values = request.getParameter("values");
			String columnLabel = request.getParameter("columnLabel");
			System.out.println("divid" + divid);

			if (count != null && !"".equalsIgnoreCase(count) && !"null".equalsIgnoreCase(count)) {
				cnt = Integer.parseInt(count);
			}
			if (filterCount != null && !"".equalsIgnoreCase(filterCount) && !"null".equalsIgnoreCase(filterCount)) {
				filterCnt = Integer.parseInt(filterCount);
			}
			String operators = "<select id ='visionVisualizeChartFiltersFieldOperatorsId" + filterCnt
					+ "' class='visionVisualizeChartFiltersOperatorsClass'>" + "<option value= 'IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "In") + "</option>"
					+ "<option value= 'Containing'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Containing") + "</option>"
					+ "<option value= 'EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Equals") + "</option>"
					+ "<option value= 'LIKE'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Like")
					+ "</option>" + "<option value= 'BEGINING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Beginning With") + "</option>"
					+ "<option value= 'ENDING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Ending With") + "</option>"
					+ "<option value= 'NOT EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Equals") + "</option>"
					+ "<option value= 'NOT IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not In") + "</option>"
					+ "<option value= 'IS'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is")
					+ "</option>" + "<option value= 'IS NOT'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is Not") + "</option>"
					+ "<option value= 'NOT LIKE'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Like") + "</option>"
					+ "<option value= 'BETWEEN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Between") + "</option>"
					+ "</select>";
			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				tableName = tableName.replace("_ID", "");
				selectQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName;
				List selectData = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), 50, 0);
				if (selectData != null && !selectData.isEmpty()) {
					result = "<div id ='visionVisualizeChartFiltersFieldDivId" + filterCnt
							+ "' class='visionVisualizeChartFiltersFieldDivClass'>"
							+ "<div class='visionVisualizeChartFiltersFieldOperator'> <div id ='visionVisualizeChartFiltersFieldId"
							+ filterCnt + "' class='visionVisualizeChartFiltersFieldsClass'>"
							+ "<input type='hidden' id='visionVisualizeChartFiltersHiddenName" + filterCnt + "' value='"
							+ tableName + "." + columnName + "'/><span class='visionVisualizeChartFiltersFieldSpan'>"
							+ ((columnLabel != null && !"".equalsIgnoreCase(columnLabel)) ? columnLabel : columnName)
							+ "</span><img src='images/close_white.png' title=\"Remove Column\" onclick=\"RemoveFilterColumns('"
							+ filterCnt + "','" + chartType + "','" + cnt + "')\"/></div>"
							+ "<div id ='visionVisualizeChartFiltersFieldOperatorsId" + filterCnt
							+ "' class='visionVisualizeChartFiltersFieldOperatorsClass'>" + operators + "</div></div>"
							// + "<div id ='visionVisualizeChartFiltersFieldValuesSearchId" + filterCnt + "'
							// class='visionVisualizeChartFiltersFieldValuesSearchClass'>"
							// + "<label>Search:</label><input type='text'
							// id='visionVisualizeChartFiltersFieldValuesSearchInputId" + filterCnt + "'
							// value=''/></div>"
							+ "<div id ='visionVisualizeChartFiltersFieldValuesId" + filterCnt
							+ "' class='visionVisualizeChartFiltersFieldValuesClass' >";
					for (int i = 0; i < selectData.size(); i++) {
						String checkBoxValue = "";
						if (selectData.get(i) instanceof String) {
							checkBoxValue = (String) selectData.get(i);
							if (checkBoxValue != null && !"".equalsIgnoreCase(checkBoxValue)
									&& !"null".equalsIgnoreCase(checkBoxValue)) {
								checkBoxValue = checkBoxValue.trim();
							}
						}
						JSONObject checkBoxData = new JSONObject();
						checkBoxData.put("text", checkBoxValue);
						checkBoxData.put("value", checkBoxValue);
						checkBoxDataArr.add(checkBoxData);

					}
					result += "</div>" + "</div>";
				}
			}
			dataObj.put("result", result);
			dataObj.put("checkBoxList", checkBoxDataArr);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	@Transactional
	public JSONObject fetchSlicerValues(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			int cnt = 0;
			String result = "";
			String selectQuery = "";
			String count = request.getParameter("count");
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("columnName");
			String divid = request.getParameter("divid");
			System.out.println("divid" + divid);

			if (count != null && !"".equalsIgnoreCase(count) && !"null".equalsIgnoreCase(count)) {
				cnt = Integer.parseInt(count);
			}

			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				selectQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName;
				List selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					result = "<div class='visionVisualizeSlicerValuesDivClass'><span class='visionVisualizeSlicerValuesSpanClass'>"
							+ columnName + "</span></div>";
					for (int i = 0; i < selectData.size(); i++) {
						result += "<input type='checkbox' class='visionVisualizeChartFiltersValuesCheckBox' name='visionVisualizeChartFiltersValuesCheckName' value='"
								+ selectData.get(i) + "'>" + selectData.get(i) + "</input>";
						if (i != selectData.size() - 1) {
							result += "<br>";
						}
					}

				}
			}
			dataObj.put("result", result);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	@Transactional
	public JSONObject fetchSlicerButtonValues(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			int cnt = 0;
			String result = "";
			String selectQuery = "";
			String count = request.getParameter("count");
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("columnName");
			String divid = request.getParameter("divid");
			String checkBoxIds = request.getParameter("checkBoxIds");
			JSONArray checkBoxIdsArr = new JSONArray();
			if (checkBoxIds != null && !"".equalsIgnoreCase(checkBoxIds) && !"null".equalsIgnoreCase(checkBoxIds)) {
				checkBoxIdsArr = (JSONArray) JSONValue.parse(checkBoxIds);
			}
			System.out.println("divid" + divid);

			if (count != null && !"".equalsIgnoreCase(count) && !"null".equalsIgnoreCase(count)) {
				cnt = Integer.parseInt(count);
			}

			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				selectQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName;
				List selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					result = "<div class='visionVisualizeSlicerValuesDivClass'><span class='visionVisualizeSlicerValuesSpanClass'>"
							+ columnName + "</span></div>";
					for (int i = 0; i < selectData.size(); i++) {
						String className = "";
						if (checkBoxIdsArr != null && !checkBoxIdsArr.isEmpty()) {
							if (selectData.get(i) instanceof BigDecimal) {
								long value = ((BigDecimal) selectData.get(i)).longValue();
								String strVal = Long.toString(value);
								if (checkBoxIdsArr.contains(strVal)) {
									className = "visionVisualizeSLicerMatchedButton";
								}
							} else if (checkBoxIdsArr.contains(selectData.get(i))) {
								className = "visionVisualizeSLicerMatchedButton";
							}
						}
						result += "<input type='button' class='visionVisualizeChartSlicersButtons " + className
								+ "'  value='" + selectData.get(i) + "'/>";
					}

				}
			}
			dataObj.put("result", result);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	@Transactional
	public JSONObject fetchSlicerListValues(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			int cnt = 0;
			String result = "";
			String selectQuery = "";
			String count = request.getParameter("count");
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("columnName");
			String divid = request.getParameter("divid");

			System.out.println("divid" + divid);

			if (count != null && !"".equalsIgnoreCase(count) && !"null".equalsIgnoreCase(count)) {
				cnt = Integer.parseInt(count);
			}

			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				selectQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName;
				List selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					result = "<div class='visionVisualizeSlicerValuesDivClass'><span class='visionVisualizeSlicerValuesSpanClass'>"
							+ columnName + "</span></div>";
					for (int i = 0; i < selectData.size(); i++) {

						result += "<input type='checkbox' class='visionVisualizeChartSlicersButtons'  value='"
								+ selectData.get(i) + "'>" + selectData.get(i) + "</input>";
						if (i != selectData.size() - 1) {
							result += "<br>";
						}
					}

				}
			}
			dataObj.put("result", result);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	@Transactional
	public JSONObject fetchSlicerDropdownValues(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			int cnt = 0;
			String result = "";
			String selectQuery = "";
			String count = request.getParameter("count");
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("columnName");
			String divid = request.getParameter("divid");
			System.out.println("divid" + divid);

			if (count != null && !"".equalsIgnoreCase(count) && !"null".equalsIgnoreCase(count)) {
				cnt = Integer.parseInt(count);
			}

			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				selectQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName;
				List selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					result = "<div class='visionVisualizeSlicerValuesDivClass'><span class='visionVisualizeSlicerValuesSpanClass'>"
							+ columnName + "</span></div>";
					result += "<select name=\"slicerOpt[]\" multiple id=\"slicerOpt\">";
					for (int i = 0; i < selectData.size(); i++) {
						result += "<option value='" + selectData.get(i) + "'>" + selectData.get(i) + "</option>";
					}
					result += "</select>";
				}
			}
			dataObj.put("result", result);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	public String buildCondition(JSONObject paramObj, HttpServletRequest request) {
		String conditionQuery = "";
		try {
			String operatorName = (String) paramObj.get("operator");
			String value = (String) paramObj.get("values");
			String columnName = (String) paramObj.get("colName");
			String tableName = (String) paramObj.get("tableName");

			if (columnName != null && columnName.endsWith("DATE")) {
				// value = "TO_DATE('" + value + "', 'MM/DD/YYYY')";
				// value = value.substring(0, value.indexOf("GMT") - 9).trim();
				if (dataBaseDriver != null && !"".equalsIgnoreCase(dataBaseDriver)) {
					if (dataBaseDriver.toUpperCase().contains("ORACLE")) {
						columnName = "TO_DATE(TO_CHAR(" + columnName + ",'DD-MM-YYYY'), 'DD-MM-YYYY')";
						value = "TO_DATE('" + value + "','DD-MM-YYYY')";
						if ("BETWEEN".equalsIgnoreCase(operatorName)) {
							String minValue = (String) paramObj.get("minvalue");
							String maxvalue = (String) paramObj.get("maxvalue");
							if (!(minValue != null && !"".equalsIgnoreCase(minValue))) {
								minValue = "01-01-1947";
							}
							if (!(maxvalue != null && !"".equalsIgnoreCase(maxvalue))) {
								Date date = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
								maxvalue = formatter.format(date);
							}
							value = "TO_DATE('" + minValue + "','DD-MM-YYYY') AND TO_DATE('" + maxvalue
									+ "','DD-MM-YYYY')";
						}
					} else if (dataBaseDriver.toUpperCase().contains("MYSQL")) {
						columnName = "STR_TO_DATE(DATE_FORMAT(" + columnName + ",'DD-MM-YYYY'), 'DD-MM-YYYY')";
						value = "STR_TO_DATE('" + value + "','DD-MM-YYYY')";
						if ("BETWEEN".equalsIgnoreCase(operatorName)) {
							String minValue = (String) paramObj.get("minvalue");
							String maxvalue = (String) paramObj.get("maxvalue");
							if (!(minValue != null && !"".equalsIgnoreCase(minValue))) {
								minValue = "01-01-1947";
							}
							if (!(maxvalue != null && !"".equalsIgnoreCase(maxvalue))) {
								Date date = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
								maxvalue = formatter.format(date);
							}
							value = "STR_TO_DATE('" + minValue + "','DD-MM-YYYY') AND STR_TO_DATE('" + maxvalue
									+ "','DD-MM-YYYY')";
						}
					} else if (dataBaseDriver.toUpperCase().contains("SQLSERVER")) {
						columnName = "CONVERT(CONVERT(VARCHAR(10)," + columnName + ",'DD-MM-YYYY'), 'DD-MM-YYYY')";
						value = "CONVERT('" + value + "','DD-MM-YYYY')";
						if ("BETWEEN".equalsIgnoreCase(operatorName)) {
							String minValue = (String) paramObj.get("minvalue");
							String maxvalue = (String) paramObj.get("maxvalue");
							if (!(minValue != null && !"".equalsIgnoreCase(minValue))) {
								minValue = "01-01-1947";
							}
							if (!(maxvalue != null && !"".equalsIgnoreCase(maxvalue))) {
								Date date = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
								maxvalue = formatter.format(date);
							}
							value = "CONVERT('" + minValue + "','DD-MM-YYYY') AND CONVERT('" + maxvalue
									+ "','DD-MM-YYYY')";
						}
					} else if (dataBaseDriver.toUpperCase().contains("DB2")) {

					}

				}

			}
			if (operatorName != null && !"".equalsIgnoreCase(operatorName) && value != null
					&& !"".equalsIgnoreCase(value)) {
				operatorName = operatorName.toUpperCase();
				switch (operatorName) {
					case "CONTAINING":
						conditionQuery = "UPPER(" + columnName + ") LIKE '%" + value + "%'";
						break;
					case "EQUALS":
						if (columnName.contains("_DATE")) {
							conditionQuery = " " + columnName + " = " + value + "";
						} else {
							conditionQuery = " " + columnName + " = '" + value + "'";
						}
						break;
					case "NOT EQUALS":
						if (columnName.contains("_DATE")) {

							conditionQuery = " " + columnName + " != " + value + "";
						} else {
							conditionQuery = " " + columnName + " != '" + value + "'";
						}
						break;

					case "GREATER THAN":
						if (columnName.contains("_DATE")) {

							conditionQuery = " " + columnName + " > " + value + "";

						} else {
							conditionQuery = " " + columnName + " > '" + value + "'";
						}
						break;
					case "LESS THAN":
						if (columnName.contains("_DATE")) {
							conditionQuery = " " + columnName + " < " + value + "";
						} else {
							conditionQuery = " " + columnName + " < '" + value + "'";
						}
						break;

					case "BEGINING WITH":
						conditionQuery = " " + columnName + " LIKE '" + value + "%'";

						break;
					case "ENDING WITH":
						conditionQuery = " " + columnName + " LIKE '%" + value + "'";
						break;
					case "LIKE":
						conditionQuery = " " + columnName + " LIKE '" + value + "'";
						break;
					case "NOT LIKE":
						conditionQuery = " " + columnName + " NOT LIKE '" + value + "'";
						break;
					case "IS":
						conditionQuery = " " + columnName + " IS  NULL";
						break;
					case "IS NOT":
						conditionQuery = " " + columnName + " IS NOT NULL";
						break;
					case ">":
						conditionQuery = " " + columnName + " > '" + value + "'";
						break;
					case "<":
						conditionQuery = " " + columnName + " < '" + value + "'";
						break;
					case ">=":
						conditionQuery = " " + columnName + " >= " + value + "";
						break;
					case "<=":
						conditionQuery = " " + columnName + " <= " + value + "";
						break;
					case "IN":

						conditionQuery = " " + columnName + " IN " + generateInStr(value) + "";
						break;
					case "NOT IN":
						conditionQuery = " " + columnName + " NOT IN " + generateInStr(value) + "";
						break;
					case "BETWEEN":
						conditionQuery = " " + columnName + " BETWEEN " + value;
						break;
					default:
						conditionQuery = " " + columnName + " " + operatorName + " " + value;
				}

			}

			// query = query + " AND " + getCondition(filterdatafield, filtercondition,
			// filtervalue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conditionQuery;
	}

	public String generateInStr(String value) {

		try {
			System.err.println("value:::Before:::" + value);
			if (value != null && !"".equalsIgnoreCase(value)) {
				value = new PilogUtilities().trimChar(value, ',');
				if (value != null && value.contains(",") && value.contains("','")) {
					value = "(" + value + ")";
				} else if (value.contains(",")) {
					value = value.trim();
					System.out.println("value is :::" + value);
					value = "('" + value.replaceAll(",", "','") + "')";
					// conditionStr = columnName + " NOT IN ('" + convertedValue.replaceAll(",",
					// "','") + "')";
				} else {
					value = "('" + value + "')";
					// conditionStr = columnName + " NOT IN ('" + convertedValue + "')";
				}
			} else {
				value = "('')";
			}
			System.err.println("value:::After:::" + value);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	public JSONObject buildOptionsObj(HttpServletRequest request, JSONObject allOptObj,
			String chartConfigPositionKeyStr, String chartId, String chartType) {
		JSONObject optionObj = new JSONObject();
		JSONObject layoutObj = new JSONObject();
		JSONObject dataObj = new JSONObject();
		JSONObject optionsKeyObj = getChartConfigStr(chartType.toUpperCase());
		try {

			JSONObject chartConfigPositionKeyobj = (JSONObject) JSONValue.parse(chartConfigPositionKeyStr);
			JSONObject chartFormObj = getChartConfigObj(chartType);
			if (chartFormObj != null && !chartFormObj.isEmpty()) {
				for (Object keyStr : chartFormObj.keySet()) {
					String key = (String) keyStr;
					String keyType = (String) chartConfigPositionKeyobj.get(key);
					if ("O".equalsIgnoreCase(String.valueOf(chartFormObj.get(key)))) {
						// need to call nested objects props
						JSONObject childOptObj = new JSONObject();
						childOptObj = buildOptionsObj(request, allOptObj, chartId, childOptObj, key, optionsKeyObj,
								chartType);
						if (childOptObj != null && !childOptObj.isEmpty()) {
//                            optionObj.put(optionsKeyObj.get(key), childOptObj.get(key));
							if (keyType != null && !"".equalsIgnoreCase(keyType) && !"null".equalsIgnoreCase(keyType)
									&& keyType.equalsIgnoreCase("data")) {
								dataObj.put(optionsKeyObj.get(key), childOptObj.get(key));
							} else if (keyType != null && !"".equalsIgnoreCase(keyType)
									&& !"null".equalsIgnoreCase(keyType) && keyType.equalsIgnoreCase("layout")) {
								layoutObj.put(optionsKeyObj.get(key), childOptObj.get(key));
							}
						}
					} else {
						if (allOptObj != null && !allOptObj.isEmpty() && allOptObj.containsKey(key)) {
							Object optValue = allOptObj.get(key);
							if (optValue != null && !"".equalsIgnoreCase(String.valueOf(optValue))
									&& !"null".equalsIgnoreCase(String.valueOf(optValue))) {

								if (optValue instanceof String) {
									String optValueStr = (String) optValue;
									if (optValueStr.contains(",")) {
										JSONArray optValuesArray = new JSONArray();
										optValuesArray.addAll(Arrays.asList(optValueStr.split(",")));
										optValue = optValuesArray;
									}
								}
//                                optionObj.put(optionsKeyObj.get(key), optValue);
								if (keyType != null && !"".equalsIgnoreCase(keyType)
										&& !"null".equalsIgnoreCase(keyType) && keyType.equalsIgnoreCase("data")) {
									dataObj.put(optionsKeyObj.get(key), optValue);
								} else if (keyType != null && !"".equalsIgnoreCase(keyType)
										&& !"null".equalsIgnoreCase(keyType) && keyType.equalsIgnoreCase("layout")) {
									layoutObj.put(optionsKeyObj.get(key), optValue);
								}
							}

						}
					}
				}
			}
			optionObj.put("layoutObj", layoutObj);
			optionObj.put("dataObj", dataObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return optionObj;
	}

	public JSONObject buildOptionsObj(HttpServletRequest request, JSONObject allOptObj, String chartId,
			JSONObject parentOptionObj, String masterColumnName, JSONObject optionsKeyObj, String chartType) {
		JSONObject optionObj = new JSONObject();
		try {
			if (allOptObj != null && !allOptObj.isEmpty()) {

				String chartTypeUppcase = chartType.toUpperCase();

				JSONObject chartFormObj = getConfigChartObj(masterColumnName, chartTypeUppcase);
				if (chartFormObj != null && !chartFormObj.isEmpty()) {
					for (Object keyStr : chartFormObj.keySet()) {
						String key = (String) keyStr;
						if ("O".equalsIgnoreCase(String.valueOf(chartFormObj.get(key)))) {
							// need to call nested objects props
							JSONObject childOptObj = new JSONObject();
							childOptObj = buildOptionsObj(request, allOptObj, chartId, childOptObj, key, optionsKeyObj,
									chartType);
							if (childOptObj != null && !childOptObj.isEmpty()) {
								optionObj.put(optionsKeyObj.get(key), childOptObj.get(key));
							}
						} else {
							if (allOptObj != null && !allOptObj.isEmpty() && allOptObj.containsKey(key)) {
								Object optValue = allOptObj.get(key);
								if (optValue != null && !"".equalsIgnoreCase(String.valueOf(optValue))
										&& !"null".equalsIgnoreCase(String.valueOf(optValue))) {
									if (key != null && !"".equalsIgnoreCase(key)
											&& ((chartTypeUppcase + "HOVERDATAECHARTS").equalsIgnoreCase(key)
													|| (chartTypeUppcase + "SLICELABELDATAECHARTS")
															.equalsIgnoreCase(key))) {
									} else if (optValue instanceof String) {
										String optValueStr = (String) optValue;
										if (optValueStr.contains(",")) {
											JSONArray optValuesArray = new JSONArray();
											optValuesArray.addAll(Arrays.asList(optValueStr.split(",")));
											optValue = optValuesArray;
										}
									}
									if (key != null && !"".equalsIgnoreCase(key)
											&& (chartTypeUppcase + "LEGENDPOSITION").equalsIgnoreCase(key)) {
										JSONObject legendPositionObj = getLegendPositions(optValue, chartType);
										for (Object position : legendPositionObj.keySet()) {
											String axisPositionValue = String.valueOf(position);
											optionObj.put(axisPositionValue, legendPositionObj.get(axisPositionValue));
										}
//                                        if (optValue != null && "Top".equalsIgnoreCase(String.valueOf(optValue))) {
//                                            optionObj.put("orientation", "h");
//                                            optionObj.put("y", 1.18);
//                                        } else if (optValue != null && "Bottom".equalsIgnoreCase(String.valueOf(optValue))) {
//                                            optionObj.put("orientation", "h");
//                                        } else if (optValue != null && "Left".equalsIgnoreCase(String.valueOf(optValue))) {
//                                            optionObj.put("x", -0.4);
//                                            optionObj.put("y", 0.6);
//                                        } else if (optValue != null && "Right".equalsIgnoreCase(String.valueOf(optValue))) {
//                                            optionObj.put("y", 0.5);
//                                        }

									} else {
										optionObj.put(optionsKeyObj.get(key), optValue);
									}

								}

							}
						}

					}
				}
				parentOptionObj.put(masterColumnName, optionObj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parentOptionObj;
	}

	public JSONObject getChartConfigObj(String chartType) {
		JSONObject configObj = new JSONObject();
		String chartTypeUppCase = chartType.toUpperCase();
		if (chartType != null && !"".equalsIgnoreCase(chartType) && "pie".equalsIgnoreCase(chartType)) {
			JSONObject pieConfigObj = new JSONObject();
			pieConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			pieConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			pieConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			pieConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			pieConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			pieConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			pieConfigObj.put(chartTypeUppCase + "MARKER", "O");
			pieConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			configObj.put(chartType, pieConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "donut".equalsIgnoreCase(chartType)) {
			JSONObject donutConfigObj = new JSONObject();
			donutConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			donutConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			donutConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			donutConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			donutConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			donutConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			donutConfigObj.put(chartTypeUppCase + "MARKER", "O");
			donutConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			configObj.put(chartType, donutConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType)
				&& ("bar".equalsIgnoreCase(chartType) || "column".equalsIgnoreCase(chartType))) {
			JSONObject barConfigObj = new JSONObject();
			barConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			barConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			barConfigObj.put(chartTypeUppCase + "MODE", "S");
			barConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
//            barConfigObj.put(chartTypeUppCase + "GAP", "S");
			barConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			barConfigObj.put(chartTypeUppCase + "SHOWLEGEND", "S");
			barConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			barConfigObj.put(chartTypeUppCase + "MARKER", "O");
			barConfigObj.put(chartTypeUppCase + "XAXIS", "O");
			barConfigObj.put(chartTypeUppCase + "YAXIS", "O");
			barConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			barConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			configObj.put(chartType, barConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "lines".equalsIgnoreCase(chartType)) {
			JSONObject linesConfigObj = new JSONObject();
			linesConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			linesConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			linesConfigObj.put(chartTypeUppCase + "MODE", "S");
			linesConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			linesConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			linesConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			linesConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			linesConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			linesConfigObj.put(chartTypeUppCase + "SHOWLEGEND", "S");
			linesConfigObj.put(chartTypeUppCase + "XAXIS", "O");
			linesConfigObj.put(chartTypeUppCase + "YAXIS", "O");
			linesConfigObj.put(chartTypeUppCase + "MARKER", "O");
			linesConfigObj.put("LINES", "O");
			configObj.put(chartType, linesConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "scatter".equalsIgnoreCase(chartType)) {
			JSONObject bubbleConfigObj = new JSONObject();
			bubbleConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			bubbleConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			bubbleConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			bubbleConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			bubbleConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			bubbleConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			bubbleConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			bubbleConfigObj.put(chartTypeUppCase + "SHOWLEGEND", "S");
			bubbleConfigObj.put(chartTypeUppCase + "XAXIS", "O");
			bubbleConfigObj.put(chartTypeUppCase + "YAXIS", "O");
			bubbleConfigObj.put(chartTypeUppCase + "MARKER", "O");
			configObj.put(chartType, bubbleConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "histogram".equalsIgnoreCase(chartType)) {
			JSONObject histogramConfigObj = new JSONObject();
			histogramConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			histogramConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			histogramConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			histogramConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			histogramConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			histogramConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			histogramConfigObj.put(chartTypeUppCase + "SHOWLEGEND", "S");
			histogramConfigObj.put(chartTypeUppCase + "MARKER", "O");
			histogramConfigObj.put(chartTypeUppCase + "XAXIS", "O");
			histogramConfigObj.put(chartTypeUppCase + "YAXIS", "O");
			histogramConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			configObj.put(chartType, histogramConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "funnel".equalsIgnoreCase(chartType)) {
			JSONObject funnelConfigObj = new JSONObject();
			funnelConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			funnelConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			funnelConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			funnelConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			funnelConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			funnelConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			funnelConfigObj.put(chartTypeUppCase + "SHOWLEGEND", "S");
			funnelConfigObj.put(chartTypeUppCase + "MARKER", "O");
			funnelConfigObj.put(chartTypeUppCase + "XAXIS", "O");
			funnelConfigObj.put(chartTypeUppCase + "YAXIS", "O");
			funnelConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			configObj.put(chartType, funnelConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "waterfall".equalsIgnoreCase(chartType)) {
			JSONObject waterfallConfigObj = new JSONObject();
			waterfallConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			waterfallConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			waterfallConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			waterfallConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			waterfallConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			waterfallConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			waterfallConfigObj.put(chartTypeUppCase + "SHOWLEGEND", "S");
			waterfallConfigObj.put(chartTypeUppCase + "MARKER", "O");
			waterfallConfigObj.put(chartTypeUppCase + "XAXIS", "O");
			waterfallConfigObj.put(chartTypeUppCase + "YAXIS", "O");
			waterfallConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			configObj.put(chartType, waterfallConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "scatterpolar".equalsIgnoreCase(chartType)) {
			JSONObject radarConfigObj = new JSONObject();
			radarConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			radarConfigObj.put(chartTypeUppCase + "LEGENDLABEL", "S");
			radarConfigObj.put(chartTypeUppCase + "HOVERLABELDATA", "S");
			radarConfigObj.put(chartTypeUppCase + "LABELDATA", "S");
			radarConfigObj.put(chartTypeUppCase + "LEGEND", "O");
			radarConfigObj.put(chartTypeUppCase + "LABELPOSITION", "S");
			radarConfigObj.put(chartTypeUppCase + "SHOWLEGEND", "S");
			radarConfigObj.put(chartTypeUppCase + "MARKER", "O");
			radarConfigObj.put(chartTypeUppCase + "XAXIS", "O");
			radarConfigObj.put(chartTypeUppCase + "YAXIS", "O");
			radarConfigObj.put(chartTypeUppCase + "HOVERLABEL", "O");
			configObj.put(chartType, radarConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "indicator".equalsIgnoreCase(chartType)) {
			JSONObject indicatorConfigObj = new JSONObject();
			indicatorConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			indicatorConfigObj.put(chartTypeUppCase + "PAPER_BGCOLOR", "S");
			indicatorConfigObj.put(chartTypeUppCase + "LEGENDFONT", "O");

			configObj.put(chartType, indicatorConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "heatMap".equalsIgnoreCase(chartType)) {
			JSONObject heatMapConfigObj = new JSONObject();
			heatMapConfigObj.put(chartTypeUppCase + "CHARTTITLE", "S");
			configObj.put(chartType, heatMapConfigObj);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "sunburst".equalsIgnoreCase(chartType)) {
			JSONObject sunburst = new JSONObject();
			sunburst.put(chartTypeUppCase + "SLICELABELECHARTS", "O");
			sunburst.put(chartTypeUppCase + "TOOLTIPECHARTS", "O");
			sunburst.put(chartTypeUppCase + "TITLEECHARTS", "S");
			configObj.put(chartType, sunburst);
		} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "treemap".equalsIgnoreCase(chartType)) {
			JSONObject treeMap = new JSONObject();
			treeMap.put(chartTypeUppCase + "SLICELABELECHARTS", "O");
			treeMap.put(chartTypeUppCase + "TOOLTIPECHARTS", "O");
			treeMap.put(chartTypeUppCase + "TITLEECHARTS", "S");
			configObj.put(chartType, treeMap);
		}
		return (JSONObject) configObj.get(chartType);
	}

	public JSONObject getConfigChartObj(String paramName, String chartType) {
		JSONObject configObj = new JSONObject();
		JSONObject legendObj = new JSONObject();
		legendObj.put(chartType + "LEGENDFONT", "O");
		legendObj.put(chartType + "LEGENDPOSITION", "S");
		configObj.put(chartType + "LEGEND", legendObj);
		JSONObject legendFontObj = new JSONObject();
		legendFontObj.put(chartType + "LEGENDFONTCOLOR", "S");
		legendFontObj.put(chartType + "LEGENDFONTFAMILY", "S");
		legendFontObj.put(chartType + "LEGENDFONTSIZE", "S");
		configObj.put(chartType + "LEGENDFONT", legendFontObj);
		JSONObject sliceMarkerObj = new JSONObject();
		sliceMarkerObj.put(chartType + "LINES", "O");
		sliceMarkerObj.put(chartType + "COLORS", "S");
		configObj.put(chartType + "MARKER", sliceMarkerObj);
		JSONObject markerLineObj = new JSONObject();
		markerLineObj.put(chartType + "LINECOLOR", "S");
		markerLineObj.put(chartType + "LINEWIDTH", "S");
		configObj.put(chartType + "LINES", markerLineObj);
		JSONObject sliceHoverObj = new JSONObject();
		sliceHoverObj.put(chartType + "HOVERBG", "S");
		sliceHoverObj.put(chartType + "HOVERBORDERCOLOR", "S");
		sliceHoverObj.put(chartType + "HOVERFONT", "O");
		configObj.put(chartType + "HOVERLABEL", sliceHoverObj);
		JSONObject sliceHoverFontObj = new JSONObject();
		sliceHoverFontObj.put(chartType + "HOVERFONTCOLOR", "S");
		sliceHoverFontObj.put(chartType + "HOVERFONTFAMILY", "S");
		sliceHoverFontObj.put(chartType + "HOVERFONTSIZE", "S");
		configObj.put(chartType + "HOVERFONT", sliceHoverFontObj);
		JSONObject xAxisObj = new JSONObject();
		xAxisObj.put(chartType + "XAXISTITLE", "S");
		xAxisObj.put(chartType + "XRANGEMODE", "S");
		xAxisObj.put(chartType + "XAXISTICKANGEL", "S");
		xAxisObj.put(chartType + "XTITLEFONT", "O");
		configObj.put(chartType + "XAXIS", xAxisObj);
		JSONObject xAxisTitleFontObj = new JSONObject();
		xAxisTitleFontObj.put(chartType + "XTITLEFONTCOLOR", "S");
		xAxisTitleFontObj.put(chartType + "XTITLEFONTFAMILY", "S");
		xAxisTitleFontObj.put(chartType + "XTITLEFONTSIZE", "S");
		configObj.put(chartType + "XTITLEFONT", xAxisTitleFontObj);
		JSONObject yAxisObj = new JSONObject();
		yAxisObj.put(chartType + "YAXISTITLE", "S");
		yAxisObj.put(chartType + "YRANGEMODE", "S");
		yAxisObj.put(chartType + "YAXISTICKANGEL", "S");
		yAxisObj.put(chartType + "YTITLEFONT", "O");
		configObj.put(chartType + "YAXIS", yAxisObj);
		JSONObject yAxisTitleFontObj = new JSONObject();
		yAxisTitleFontObj.put(chartType + "YTITLEFONTCOLOR", "S");
		yAxisTitleFontObj.put(chartType + "YTITLEFONTFAMILY", "S");
		yAxisTitleFontObj.put(chartType + "YTITLEFONTSIZE", "S");
		configObj.put(chartType + "YTITLEFONT", yAxisTitleFontObj);
		JSONObject lineObj = new JSONObject();
		lineObj.put(chartType + "COLORS", "S");
		lineObj.put(chartType + "WIDTH", "S");
		lineObj.put(chartType + "DASH", "S");
		lineObj.put(chartType + "SHAPE", "S");
		configObj.put("LINES", lineObj);
		JSONObject markerObj = new JSONObject();
		markerObj.put(chartType + "COLORSMARKER", "S");
		markerObj.put(chartType + "MARKERSIZE", "S");
		configObj.put("LINESMARKER", markerObj);
		JSONObject bubbleMarkerObj = new JSONObject();
		bubbleMarkerObj.put(chartType + "COLORSMARKER", "S");
		bubbleMarkerObj.put(chartType + "OPACITY", "S");
		bubbleMarkerObj.put(chartType + "MARKERSIZE", "S");
		configObj.put("SCATTERMARKER", bubbleMarkerObj);
		// Echarts
		JSONObject sliceLabelEcharts = new JSONObject();
		sliceLabelEcharts.put(chartType + "LABELROTATEECHARTS", "S");
		sliceLabelEcharts.put(chartType + "SLICELABELDATAECHARTS", "S");
		sliceLabelEcharts.put(chartType + "LABELPOSITIONECHARTS", "S");
		sliceLabelEcharts.put(chartType + "LABELFONTWIDTHECHARTS", "S");
		sliceLabelEcharts.put(chartType + "LABELOVERFLOWECHARTS", "S");
		sliceLabelEcharts.put(chartType + "LABELFONTCOLORECHARTS", "S");
		sliceLabelEcharts.put(chartType + "LABELFONTSIZEECHARTS", "S");
		sliceLabelEcharts.put(chartType + "LABELFONTFAMILYECHARTS", "S");
		configObj.put(chartType + "SLICELABELECHARTS", sliceLabelEcharts);
		JSONObject tooltipEcharts = new JSONObject();
		tooltipEcharts.put(chartType + "BACKGROUNDCOLORECHARTS", "S");
		tooltipEcharts.put(chartType + "HOVERDATAECHARTS", "S");
		tooltipEcharts.put(chartType + "TEXTSTYLEECHARTS", "O");
		configObj.put(chartType + "TOOLTIPECHARTS", tooltipEcharts);
		JSONObject textStyleEcharts = new JSONObject();
		textStyleEcharts.put(chartType + "FONTCOLORECHARTS", "S");
		textStyleEcharts.put(chartType + "FONTSIZEECHARTS", "S");
		textStyleEcharts.put(chartType + "FONTFAMILYECHARTS", "S");
		configObj.put(chartType + "TEXTSTYLEECHARTS", textStyleEcharts);
		return (JSONObject) configObj.get(paramName);
	}

	public JSONObject getChartConfigStr(String chartType) {
		JSONObject configObj = new JSONObject();
		configObj.put(chartType + "CHARTTITLE", "title");
		configObj.put(chartType + "LEGENDLABEL", "legendLabel");
		configObj.put(chartType + "LABELDATA", "textinfo");
		configObj.put(chartType + "LABELPOSITION", "textposition");
		configObj.put(chartType + "HOVERLABELDATA", "hoverinfo");
		configObj.put(chartType + "LEGEND", "legend");
		configObj.put(chartType + "SHOWLEGEND", "showlegend");
		configObj.put(chartType + "LEGENDPOSITION", "position");
		configObj.put(chartType + "ORIENTATION", "orientation");
		configObj.put(chartType + "LEGENDFONT", "font");
		configObj.put(chartType + "LEGENDFONTCOLOR", "color");
		configObj.put(chartType + "LEGENDFONTFAMILY", "family");
		configObj.put(chartType + "LEGENDFONTSIZE", "size");
		configObj.put(chartType + "MARKER", "marker");
		configObj.put(chartType + "COLORS", "colors");
		configObj.put(chartType + "LINES", "line");
		configObj.put(chartType + "LINECOLOR", "color");
		configObj.put(chartType + "LINEWIDTH", "width");
//        configObj.put("LINES", "line");
//        configObj.put(chartType + "COLOR", "color");
//        configObj.put(chartType + "WIDTH", "width");
		configObj.put(chartType + "HOVERLABEL", "hoverlabel");
		configObj.put(chartType + "HOVERBG", "bgcolor");
		configObj.put(chartType + "HOVERBORDERCOLOR", "bordercolor");
		configObj.put(chartType + "HOVERFONT", "font");
		configObj.put(chartType + "HOVERFONTCOLOR", "color");
		configObj.put(chartType + "HOVERFONTFAMILY", "family");
		configObj.put(chartType + "HOVERFONTSIZE", "size");
//        configObj.put(chartType + "GAP", "bargap");
		configObj.put(chartType + "XAXIS", "xaxis");
		configObj.put(chartType + "YAXIS", "yaxis");
		configObj.put(chartType + "XAXISTITLE", "title");
		configObj.put(chartType + "YAXISTITLE", "title");
		configObj.put(chartType + "XRANGEMODE", "rangemode");
		configObj.put(chartType + "YRANGEMODE", "rangemode");
		configObj.put(chartType + "XAXISTICKANGEL", "tickangle");
		configObj.put(chartType + "YAXISTICKANGEL", "tickangle");
		configObj.put(chartType + "XTITLEFONT", "titlefont");
		configObj.put(chartType + "XTITLEFONTCOLOR", "color");
		configObj.put(chartType + "XTITLEFONTFAMILY", "family");
		configObj.put(chartType + "XTITLEFONTSIZE", "size");
		configObj.put(chartType + "YTITLEFONT", "titlefont");
		configObj.put(chartType + "YTITLEFONTCOLOR", "color");
		configObj.put(chartType + "YTITLEFONTFAMILY", "family");
		configObj.put(chartType + "YTITLEFONTSIZE", "size");
		configObj.put(chartType + "PAPER_BGCOLOR", "paper_bgcolor");
//        configObj.put(chartType + "MODE", "mode");
//        configObj.put(chartType + "MARKER", "marker");
//        configObj.put(chartType + "MARKERCOLOR", "color");
//        configObj.put(chartType + "MARKERSIZE", "size");
//        configObj.put(chartType + "LINES", "line");
//        configObj.put(chartType + "WIDTH", "width");
//        configObj.put(chartType + "DASH", "dash");
//        configObj.put(chartType + "SHAPE", "shape");
//        configObj.put(chartType + "MARKER", "marker");
//        configObj.put(chartType + "COLOR", "color");
//        configObj.put(chartType + "OPACITY", "opacity");
//        configObj.put(chartType + "SIZE", "size");
		if (chartType != null && !"".equalsIgnoreCase(chartType)
				&& ("pie".equalsIgnoreCase(chartType) || "donut".equalsIgnoreCase(chartType)
						|| ("bar".equalsIgnoreCase(chartType) || "column".equalsIgnoreCase(chartType)))) {
			configObj.put(chartType + "LINES", "line");
			configObj.put(chartType + "LINECOLOR", "color");
			configObj.put(chartType + "LINEWIDTH", "width");
		}
		if (chartType != null && !"".equalsIgnoreCase(chartType)
				&& ("bar".equalsIgnoreCase(chartType) || "column".equalsIgnoreCase(chartType))) {
			configObj.put(chartType + "GAP", "bargap");
			configObj.put(chartType + "MODE", "barmode");
		}
		if (chartType != null && !"".equalsIgnoreCase(chartType)
				&& ("lines".equalsIgnoreCase(chartType) || "scatter".equalsIgnoreCase(chartType))) {
			configObj.put(chartType + "MODE", "mode");
			configObj.put(chartType + "MARKER", "marker");
			configObj.put(chartType + "COLORSMARKER", "color");
			configObj.put(chartType + "MARKERSIZE", "size");
			configObj.put(chartType, "line");
			configObj.put(chartType + "COLORS", "color");
			configObj.put(chartType + "WIDTH", "width");
			configObj.put(chartType + "DASH", "dash");
			configObj.put(chartType + "SHAPE", "shape");
			configObj.put(chartType + "OPACITY", "opacity");
		}

		// Echarts
		configObj.put(chartType + "TITLEECHARTS", "text");
		configObj.put(chartType + "SLICELABELECHARTS", "label");
		configObj.put(chartType + "SLICELABELDATAECHARTS", "formatter");
		configObj.put(chartType + "LABELPOSITIONECHARTS", "position");
		configObj.put(chartType + "LABELROTATEECHARTS", "rotate");
		configObj.put(chartType + "LABELFONTWIDTHECHARTS", "width");
		configObj.put(chartType + "LABELOVERFLOWECHARTS", "overflow");
		configObj.put(chartType + "LABELFONTCOLORECHARTS", "color");
		configObj.put(chartType + "LABELFONTSIZEECHARTS", "fontSize");
		configObj.put(chartType + "LABELFONTFAMILYECHARTS", "fontFamily");
		configObj.put(chartType + "TOOLTIPECHARTS", "tooltip");
		configObj.put(chartType + "BACKGROUNDCOLORECHARTS", "backgroundColor");
		configObj.put(chartType + "HOVERDATAECHARTS", "formatter");
		configObj.put(chartType + "TEXTSTYLEECHARTS", "textStyle");
		configObj.put(chartType + "FONTCOLORECHARTS", "color");
		configObj.put(chartType + "FONTSIZEECHARTS", "fontSize");
		configObj.put(chartType + "FONTFAMILYECHARTS", "fontFamily");
		return configObj;
	}

	public JSONObject getLegendPositions(Object optValue, String chartType) {
		JSONObject optionObj = new JSONObject();
		if (optValue != null && !"".equalsIgnoreCase(String.valueOf(optValue))
				&& !"null".equalsIgnoreCase(String.valueOf(optValue))) {
			if (chartType != null && !"".equalsIgnoreCase(chartType)
					&& ("pie".equalsIgnoreCase(chartType) || "donut".equalsIgnoreCase(chartType))) {
				if (optValue != null && "Top".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("orientation", "h");
					optionObj.put("y", 1.12);
				} else if (optValue != null && "Bottom".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("orientation", "h");
				} else if (optValue != null && "Left".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("x", -0.9);
					optionObj.put("y", 0.6);
				} else if (optValue != null && "Right".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("y", 0.5);
				}
			} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "bar".equalsIgnoreCase(chartType)) {
				if (optValue != null && "Top".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("orientation", "h");
					optionObj.put("y", 1.18);
				} else if (optValue != null && "Bottom".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("x", -0.3);
					optionObj.put("y", -0.3);
					optionObj.put("orientation", "h");
				} else if (optValue != null && "Left".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("x", -0.2);
					optionObj.put("y", 0.5);
				} else if (optValue != null && "Right".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("y", 0.5);
				}
			} else {
				if (optValue != null && "Top".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("orientation", "h");
					optionObj.put("y", 1.18);
				} else if (optValue != null && "Bottom".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("orientation", "h");
				} else if (optValue != null && "Left".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("x", -0.4);
					optionObj.put("y", 0.6);
				} else if (optValue != null && "Right".equalsIgnoreCase(String.valueOf(optValue))) {
					optionObj.put("y", 0.5);
				}
			}
		}
		return optionObj;
	}

	public List<Object[]> getTreeOracleTableColumns(HttpServletRequest request, String tableName) {
		List<Object[]> sourceColumnsList = new ArrayList<>();
		try {
			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)) {
				tableName = tableName.replaceAll(",", "','");
				System.out.println("tableName:::" + tableName);
				String query = "SELECT DISTINCT TABLE_NAME,COLUMN_NAME FROM USER_TAB_COLUMNS WHERE "
						+ " TABLE_NAME IN ('" + tableName + "') ORDER BY TABLE_NAME";
				Map<String, Object> selectMap = new HashMap<>();
				sourceColumnsList = access.sqlqueryWithParams(query, selectMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sourceColumnsList;
	}

	@Transactional
	public JSONObject fetchCardDetails(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			String result = "";
			String selectQuery = "";
			String imageEncodedString = "images/pricebasket1.png";
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String filterColumns = request.getParameter("filterColumns");
			String toFilterColumns = request.getParameter("toFilterArr");
			String fromFilterColumns = request.getParameter("fromFilterArr");
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("columnName");
			String dateColumnName = request.getParameter("dateColumnName");
			String type = request.getParameter("type");
			String Slicecolumn = request.getParameter("SliceColumn");
			String selectedvalue = request.getParameter("selectedValue");
			String cardType = request.getParameter("cardType");
			String cardTrend = request.getParameter("cardTrend");
			String cardCount = request.getParameter("count");
			String visualizeAreaCardImageName = request.getParameter("visualizeAreaCardImageName");
			JSONArray filterColsArr = new JSONArray();
			JSONArray fromFilterArr = new JSONArray();
			JSONArray toFilterArr = new JSONArray();
			JSONObject chartConfigObj = new JSONObject();
			String whereCondQuery = "";
			String fromWhereCondQuery = "";
			String toWhereCondQuery = "";
			String date = "";

			LocalDate now = LocalDate.now();
			LocalDate earlier = now.minusMonths(1);
			LocalDate earlierDay = earlier.minusDays(1);
			LocalDate earlierMonth = earlierDay.minusMonths(1);

			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (fromFilterColumns != null && !"".equalsIgnoreCase(fromFilterColumns)
					&& !"null".equalsIgnoreCase(fromFilterColumns)) {
				fromFilterArr = (JSONArray) JSONValue.parse(fromFilterColumns);
			}
			if (toFilterColumns != null && !"".equalsIgnoreCase(toFilterColumns)
					&& !"null".equalsIgnoreCase(toFilterColumns)) {
				toFilterArr = (JSONArray) JSONValue.parse(toFilterColumns);
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			JSONArray trendLabelsArr = new JSONArray();
			if (fromFilterArr != null && !fromFilterArr.isEmpty()) {
				for (int i = 0; i < fromFilterArr.size(); i++) {
					JSONObject fromFilterColObj = (JSONObject) fromFilterArr.get(i);
					if (fromFilterColObj != null && !fromFilterColObj.isEmpty()) {
						if (fromFilterColObj.containsKey("values")) {
							String values = (String) fromFilterColObj.get("values");
							trendLabelsArr.add(values);
							date += values + " - ";
						} else {
							String maxValue = (String) fromFilterColObj.get("maxvalue");
							String minValue = (String) fromFilterColObj.get("minvalue");
							trendLabelsArr.add("fromDate");
							date += minValue + " TO " + maxValue + " - ";

						}
						fromWhereCondQuery += buildCondition(fromFilterColObj, request);
						if (i != fromFilterArr.size() - 1) {
							fromWhereCondQuery += " AND ";
						}
					}
				}
			}
			if (toFilterArr != null && !toFilterArr.isEmpty()) {
				for (int i = 0; i < toFilterArr.size(); i++) {
					JSONObject toFilterColObj = (JSONObject) toFilterArr.get(i);
					if (toFilterColObj != null && !toFilterColObj.isEmpty()) {
						if (toFilterColObj.containsKey("values")) {
							String values = (String) toFilterColObj.get("values");
							trendLabelsArr.add(values);
							date += values;
						} else {
							String maxValue = (String) toFilterColObj.get("maxvalue");
							String minValue = (String) toFilterColObj.get("minvalue");
							trendLabelsArr.add("toDate");
							date += minValue + " TO " + maxValue;
						}
						toWhereCondQuery += buildCondition(toFilterColObj, request);
						if (i != toFilterArr.size() - 1) {
							toWhereCondQuery += " AND ";
						}
					}
				}
			}
			if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += Slicecolumn + " ";
				whereCondQuery += "IN";
				whereCondQuery += value;
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
				if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery)) {
					fromWhereCondQuery = whereCondQuery + " AND " + fromWhereCondQuery;
				}
				if (toWhereCondQuery != null && !"".equalsIgnoreCase(toWhereCondQuery)) {
					toWhereCondQuery = whereCondQuery + " AND " + toWhereCondQuery;
				}
			} else {
				if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery)) {
					fromWhereCondQuery = " WHERE " + fromWhereCondQuery;
				}
				if (toWhereCondQuery != null && !"".equalsIgnoreCase(toWhereCondQuery)) {
					toWhereCondQuery = " WHERE " + toWhereCondQuery;
				}
			}
			JSONArray trendDataArr = new JSONArray();
			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				long fromCount = 0;
				long toCount = 0;
				long totalCount = 0;
				String percent = "";
				if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery) && toWhereCondQuery != null
						&& !"".equalsIgnoreCase(toWhereCondQuery)) {
					if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery)) {
						String fromQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName
								+ fromWhereCondQuery;
						List fromCountList = access.sqlqueryWithParams(fromQuery, new HashMap());
						if (fromCountList != null && !fromCountList.isEmpty()) {
							fromCount = new PilogUtilities().convertIntoInteger(fromCountList.get(0));
						}
					}
					if (toWhereCondQuery != null && !"".equalsIgnoreCase(toWhereCondQuery)) {
						String toQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + toWhereCondQuery;
						List toCountList = access.sqlqueryWithParams(toQuery, new HashMap());
						if (toCountList != null && !toCountList.isEmpty()) {
							toCount = new PilogUtilities().convertIntoInteger(toCountList.get(0));
						}
					}
					if (type != null && !"".equalsIgnoreCase(type) && !"UniqueCount".equals(type)) {
						selectQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + whereCondQuery;
					} else {
						selectQuery = "SELECT COUNT(*) AS VALUE FROM (SELECT  DISTINCT " + columnName + " FROM "
								+ tableName + whereCondQuery + ")";
					}

					List countList = access.sqlqueryWithParams(selectQuery, new HashMap());
					if (countList != null && !countList.isEmpty()) {
						totalCount = new PilogUtilities().convertIntoInteger(countList.get(0));
					}
					trendDataArr.add(fromCount);
					trendDataArr.add(toCount);
					if (fromCount > toCount) {
						long diff = (fromCount - toCount);
						double percentage = (diff * 100) / fromCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/thumbsdown.png' class='icon' style='width: 30px;'></span>";
					} else if (toCount > fromCount) {
						long diff = (toCount - fromCount);
						double percentage = (diff * 100) / toCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/like.png' class='icon' style='width: 30px;'></span>";
					}
				} else if (dateColumnName != null && !"".equalsIgnoreCase(dateColumnName)) {
					fromWhereCondQuery = "TO_DATE(TO_CHAR(" + dateColumnName
							+ ",'YYYY-MM-DD'), 'YYYY-MM-DD') BETWEEN TO_DATE('" + earlierMonth
							+ "','YYYY-MM-DD') AND TO_DATE('" + earlierDay + "','YYYY-MM-DD')";
					toWhereCondQuery = "TO_DATE(TO_CHAR(" + dateColumnName
							+ ",'YYYY-MM-DD'), 'YYYY-MM-DD') BETWEEN TO_DATE('" + earlier
							+ "','YYYY-MM-DD') AND TO_DATE('" + now + "','YYYY-MM-DD')";
					if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)) {
						fromWhereCondQuery = whereCondQuery + " AND " + fromWhereCondQuery;
						toWhereCondQuery = whereCondQuery + " AND " + toWhereCondQuery;
					} else {
						fromWhereCondQuery = " WHERE " + fromWhereCondQuery;
						toWhereCondQuery = " WHERE " + toWhereCondQuery;
					}
					String fromQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + fromWhereCondQuery;
					List fromCountList = access.sqlqueryWithParams(fromQuery, new HashMap());
					if (fromCountList != null && !fromCountList.isEmpty()) {
						fromCount = new PilogUtilities().convertIntoInteger(fromCountList.get(0));
					}
					String toQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + toWhereCondQuery;
					List toCountList = access.sqlqueryWithParams(toQuery, new HashMap());
					if (toCountList != null && !toCountList.isEmpty()) {
						toCount = new PilogUtilities().convertIntoInteger(toCountList.get(0));
					}
					if (type != null && !"".equalsIgnoreCase(type) && !"UniqueCount".equals(type)) {
						selectQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + whereCondQuery;
					} else {
						selectQuery = "SELECT COUNT(*) AS VALUE FROM (SELECT  DISTINCT " + columnName + " FROM "
								+ tableName + whereCondQuery + ")";
					}

					List countList = access.sqlqueryWithParams(selectQuery, new HashMap());
					if (countList != null && !countList.isEmpty()) {
						totalCount = new PilogUtilities().convertIntoInteger(countList.get(0));
					}
					trendDataArr.add(fromCount);
					trendDataArr.add(toCount);
					if (fromCount > toCount) {
						long diff = (fromCount - toCount);
						double percentage = (diff * 100) / fromCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/thumbsdown.png' class='icon' style='width: 30px;'></span>";
					} else if (toCount > fromCount) {
						long diff = (toCount - fromCount);
						double percentage = (diff * 100) / toCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/like.png' class='icon' style='width: 30px;'></span>";
					}
				} else {

					if (type != null && !"".equalsIgnoreCase(type) && !"UniqueCount".equals(type)) {
						selectQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + whereCondQuery;
					} else {
						selectQuery = "SELECT COUNT(*) AS VALUE FROM (SELECT  DISTINCT " + columnName + " FROM "
								+ tableName + whereCondQuery + ")";
					}

					List countList = access.sqlqueryWithParams(selectQuery, new HashMap());
					if (countList != null && !countList.isEmpty()) {
						totalCount = new PilogUtilities().convertIntoInteger(countList.get(0));
					}
				}

				String colLabel = columnName.toLowerCase().replace("_", " ");
				String columnNameTitleCase = dashboardutils.convertTextToTitleCase(colLabel);
				if (!dashboardutils.isNullOrEmpty(visualizeAreaCardImageName)) {
					String userName = (String) request.getSession(false).getAttribute("ssUsername");
					String fileDirectoryOnserver = fileStoreHomedirectory + "images/" + userName;
					String fileExtension = FilenameUtils.getExtension(visualizeAreaCardImageName);
					String imageEncodedCode = dashboardutils.getImageBase64EncodedString(fileDirectoryOnserver,
							visualizeAreaCardImageName);
					if (!dashboardutils.isNullOrEmpty(fileExtension)) {
						if ("svg".equalsIgnoreCase(fileExtension)) {
							String imageHeader = "data:image/" + fileExtension + "+xml;base64,";
							imageEncodedString = imageHeader + imageEncodedCode;
						} else {
							String imageHeader = "data:image/" + fileExtension + ";base64,";
							imageEncodedString = imageHeader + imageEncodedCode;
						}
					}
				}
				if (totalCount >= 0) {
					String datacount = withSuffix(totalCount);
					
					if(cardTrend !=null && !"".equalsIgnoreCase(cardTrend) && "Trend".equalsIgnoreCase(cardTrend))
					{
						if (cardType != null && !"".equalsIgnoreCase(cardType) && "Rectangle".equalsIgnoreCase(cardType)) {
							result += "<div class='visionVisualizeCardValuesDivClass'>" + "<div class='titleMainClass'>"
//									+ "<span class=\"visionVisualizeCardLevelSpanClass\">" + columnName + "</span>"
									+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
									+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
									+ "  </span>" + "</div>" + "<div class='mainCardClass'>"
									+ "<div class='align-self-center'>"
//									+ "<img id=\"cardImageVisualizeArea\" src='images/pricebasket1.png' class='icon' style='width:30px; margin-left: 10px;' onclick=\"encodeImageFileAndAppendAsSrc(this)\">"
									+ "<img id=\"cardImageVisualizeArea\" src='" + imageEncodedString
									+ "' class='icon' style='width:30px; margin-left: 10px;' onclick=\"encodeImageFileAndAppendAsSrc(this)\">"
//									+ "<input type='file' name='importCardImage' id='importCardImage' style='display:none;'/>"
									+ "</div>" + "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>"
									+ "<div id='visionVisualizeCardTrendGraph" + cardCount + "' class='align-self-center'>"
									+ " <img src='images/cardGraphImage.png' class='icon' style='width:30px;'>" + "</div>"
									+ "</div>" + "<div class='thumbsDateClass'>"
									+ "<div class='plotlyRightImage' style='text-align: center;'>" + percent + "</div>"
									+ "<div class='todayDate'>" + "<span class=\"todayDate\">" + date + "</span>" + "</div>"
									+ "</div>" + "</div>";
						} else if (cardType != null && !"".equalsIgnoreCase(cardType)
								&& "Oval".equalsIgnoreCase(cardType)) {
							result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesOval'>"
									+ "<div class='titleMainClass'>"
//									+ "<span class=\"visionVisualizeCardLevelSpanClass\">"+ columnName + "</span>"
									+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
									+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
									+ "  </span>" + "</div>" + "<div class='mainCardClass'>"
									+ "<div class='align-self-center'>"
//									+ "<img id=\"cardImageVisualizeArea\" src='images/pricebasket1.png' class='icon' style='width:30px; margin-left: 10px;' onclick=\"encodeImageFileAndAppendAsSrc(this)\">"
									+ "<img id=\"cardImageVisualizeArea\" src='" + imageEncodedString
									+ "' class='icon' style='width:30px; margin-left: 10px;' onclick=\"encodeImageFileAndAppendAsSrc(this)\">"
//									+ "<input type='file' name='importCardImage' id='importCardImage' style='display:none;'/>"
									+ "</div>" + "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>"
									+ "<div id='visionVisualizeCardTrendGraph" + cardCount + "' class='align-self-center'>"
									+ " <img src='images/cardGraphImage.png' class='icon' style='width:30px; margin-right: 10px;'>"
									+ "</div>" + "</div>" + "<div class='thumbsDateClass'>"
									+ "<div class='plotlyRightImage' style='text-align: center;'>" + percent + "</div>"
									+ "<div class='todayDate'>" + "<span class=\"todayDate\">" + date + "</span>" + "</div>"
									+ "</div>" + "</div>";
						} else if (cardType != null && !"".equalsIgnoreCase(cardType)
								&& "Round".equalsIgnoreCase(cardType)) {
							result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesRound'>"
									+ "<div class='titleMainClass'>"
//									+ "<span class=\"visionVisualizeCardLevelSpanClass\">"+ columnName + "</span>"
									+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
									+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
									+ "  </span>" + "</div>" + "<div class='mainCardClass'>"
									+ "<div class='align-self-center'>"
//									+ "<img id=\"cardImageVisualizeArea\" src='images/pricebasket1.png' class='icon' style='width:30px; margin-left: 10px;' onclick=\"encodeImageFileAndAppendAsSrc(this)\">"
									+ "<img id=\"cardImageVisualizeArea\" src='" + imageEncodedString
									+ "' class='icon' style='width:30px; margin-left: 10px;' onclick=\"encodeImageFileAndAppendAsSrc(this)\">"
//									+ "<input type='file' name='importCardImage' id='importCardImage' style='display:none;'/>"
									+ "</div>" + "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>"
									+ "<div id='visionVisualizeCardTrendGraph" + cardCount + "' class='align-self-center'>"
									+ " <img src='images/cardGraphImage.png' class='icon' style='width:30px;'>" + "</div>"
									+ "</div>" + "<div class='thumbsDateClass'>"
									+ "<div class='plotlyRightImage' style='text-align: center;'>" + percent + "</div>"
									+ "<div class='todayDate'>" + "<span class=\"todayDate\">" + date + "</span>" + "</div>"
									+ "</div>" + "</div>";
						} else if (cardType != null && !"".equalsIgnoreCase(cardType)
								&& "Normal".equalsIgnoreCase(cardType)) {
							result += "<div class='visionVisualizeCardValuesLevelDivClass'>"
									+ "<div class='titleMainLevelClass'>"
//									+ "<span class=\"visionVisualizeCardLevelSpanClass\">" + columnName + "</span>"
									+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
									+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
									+ "  </span>" + "</div>" + "<div class='mainCardClass'>"
									+ "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>" + "</div>"
									+ "</div>";
						}
					}
					else {
					if (cardType != null && !"".equalsIgnoreCase(cardType) && "Rectangle".equalsIgnoreCase(cardType)) {
						result += "<div class='visionVisualizeCardValuesDivClass'>" + "<div class='titleMainClass'>"
//								+ "<span class=\"visionVisualizeCardLevelSpanClass\">" + columnName + "</span>"
								+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
								+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
								+ "  </span>" + "</div>" 
								+ "<div class='mainCardClass'>"
								+ "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>"
								+ "</div>" 
								+ "</div>";
					} else if (cardType != null && !"".equalsIgnoreCase(cardType)
							&& "Oval".equalsIgnoreCase(cardType)) {
						result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesOval'>"
								+ "<div class='titleMainClass'>"
//								+ "<span class=\"visionVisualizeCardLevelSpanClass\">"+ columnName + "</span>"
								+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
								+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
								+ "  </span>" + "</div>" 
								+ "<div class='mainCardClass'>"
								+ "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>"
								+ "</div>" 
								+ "</div>";
					} else if (cardType != null && !"".equalsIgnoreCase(cardType)
							&& "Round".equalsIgnoreCase(cardType)) {
						result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesRound'>"
								+ "<div class='titleMainClass'>"
//								+ "<span class=\"visionVisualizeCardLevelSpanClass\">"+ columnName + "</span>"
								+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
								+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
								+ "  </span>" + "</div>" 
								+ "<div class='mainCardClass'>"
								+ "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>"
								+ "</div>" 
								+ "</div>";
					} else if (cardType != null && !"".equalsIgnoreCase(cardType)
							&& "Normal".equalsIgnoreCase(cardType)) {
						result += "<div class='visionVisualizeCardValuesLevelDivClass'>"
								+ "<div class='titleMainLevelClass'>"
//								+ "<span class=\"visionVisualizeCardLevelSpanClass\">" + columnName + "</span>"
								+ " <input type=\"text\" class=\"visionVisualizeCardLevelSpanClass\" id='visionVisualizeCardTitle' value=\""
								+ columnNameTitleCase + "\">" + "<span id='Cardtype' style='display: none;'>" + type
								+ "  </span>" + "</div>" + "<div class='mainCardClass'>"
								+ "<div class='visionVisualizeCardValuesSpanClass'>" + datacount + "</div>" + "</div>"
								+ "</div>";
					}
					}
				}
			}
			dataObj.put("result", result);
			dataObj.put("trendDataArr", trendDataArr);
			dataObj.put("trendLabelsArr", trendLabelsArr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	@Transactional
	public JSONObject fetchHomeCardDetails(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			Random rand = new Random();
			String result = "";
			String selectQuery = "";
			String dashboardCardTitle = "";
			Clob cardImageClob = null;
			String cardImageEncodedString = "";
			String cardImageTag = "";
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String filterColumns = request.getParameter("filterColumns");
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("columnName");
			String type = request.getParameter("type");
			String Slicecolumn = request.getParameter("SliceColumn");
			String selectedvalue = request.getParameter("selectedValue");
			String filterCondition = request.getParameter("filterCondition");
			String toFilterColumns = request.getParameter("toFilterArr");
			String fromFilterColumns = request.getParameter("fromFilterArr");
			String dateColumnName = request.getParameter("paramDateArr");
			String Lebel = request.getParameter("Lebel");
			String cardType = request.getParameter("cardType");
			String cardTrendType = request.getParameter("cardTrendType");
			String cardTrend = request.getParameter("cardTrend");
			String dashboardCardId = request.getParameter("cardId");
			String date = "";
			String isApplyEvt = request.getParameter("isApplyEvt");

			LocalDate now = LocalDate.now();
			LocalDate earlier = now.minusMonths(1);
			LocalDate earlierDay = earlier.minusDays(1);
			LocalDate earlierMonth = earlierDay.minusMonths(1);

			JSONObject paramFromObj = new JSONObject();
			paramFromObj.put("colName", columnName);
			paramFromObj.put("operator", "BETWEEN");
			paramFromObj.put("minvalue", earlierMonth);
			paramFromObj.put("maxvalue", earlierDay);
			JSONObject paramToObj = new JSONObject();
			paramToObj.put("colName", columnName);
			paramToObj.put("operator", "BETWEEN");
			paramToObj.put("minvalue", earlier);
			paramToObj.put("maxvalue", now);

			JSONArray filterColsArr = new JSONArray();
			JSONArray fromFilterArr = new JSONArray();
			JSONArray toFilterArr = new JSONArray();
			JSONObject chartConfigObj = new JSONObject();
			String whereCondQuery = "";
			String fromWhereCondQuery = "";
			String toWhereCondQuery = "";
			JSONArray trendLabelsArr = new JSONArray();
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (fromFilterColumns != null && !"".equalsIgnoreCase(fromFilterColumns)
					&& !"null".equalsIgnoreCase(fromFilterColumns)) {
				fromFilterArr = (JSONArray) JSONValue.parse(fromFilterColumns);
			}
			if (toFilterColumns != null && !"".equalsIgnoreCase(toFilterColumns)
					&& !"null".equalsIgnoreCase(toFilterColumns)) {
				toFilterArr = (JSONArray) JSONValue.parse(toFilterColumns);
			}
			if (dateColumnName != null && !"".equalsIgnoreCase(dateColumnName)
					&& !"".equalsIgnoreCase(dateColumnName)) {
				JSONObject dateColumnObj = (JSONObject) JSONValue.parse(dateColumnName);
				if (dateColumnObj != null && !dateColumnObj.isEmpty()) {
					JSONArray dataColumnArr = (JSONArray) dateColumnObj.get("dateColumnData");
					if (dataColumnArr != null && !dataColumnArr.isEmpty()) {
						dateColumnName = (String) dataColumnArr.get(0);
					}
				}
			}
			if (filterCondition != null && !"".equalsIgnoreCase(filterCondition)
					&& !"null".equalsIgnoreCase(filterCondition)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterCondition);
			}

			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}
			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);

					if (filterColObj.get("values") instanceof JSONArray) {
						JSONArray valuesArr = (JSONArray) filterColObj.get("values");
						if (valuesArr != null && !valuesArr.isEmpty()) {
							String values = (String) valuesArr.stream().map(e -> e).collect(Collectors.joining(","));
							filterColObj.put("values", values);
						}
					}

					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (fromFilterArr != null && !fromFilterArr.isEmpty()) {
				for (int i = 0; i < fromFilterArr.size(); i++) {
					JSONObject fromFilterColObj = (JSONObject) fromFilterArr.get(i);
					if (fromFilterColObj != null && !fromFilterColObj.isEmpty()) {
						if (fromFilterColObj.containsKey("values")) {
							String values = (String) fromFilterColObj.get("values");
							trendLabelsArr.add(values);
							date += values + " - ";
						} else {
							String maxValue = (String) fromFilterColObj.get("maxvalue");
							String minValue = (String) fromFilterColObj.get("minvalue");
							trendLabelsArr.add("fromDate");
							date += minValue + " TO " + maxValue + " - ";

						}
						fromWhereCondQuery += buildCondition(fromFilterColObj, request);
						if (i != fromFilterArr.size() - 1) {
							fromWhereCondQuery += " AND ";
						}
					}
				}
			}
			if (toFilterArr != null && !toFilterArr.isEmpty()) {
				for (int i = 0; i < toFilterArr.size(); i++) {
					JSONObject toFilterColObj = (JSONObject) toFilterArr.get(i);
					if (toFilterColObj != null && !toFilterColObj.isEmpty()) {
						if (toFilterColObj.containsKey("values")) {
							String values = (String) toFilterColObj.get("values");
							trendLabelsArr.add(values);
							date += values;
						} else {
							String maxValue = (String) toFilterColObj.get("maxvalue");
							String minValue = (String) toFilterColObj.get("minvalue");
							trendLabelsArr.add("toDate");
							date += minValue + " TO " + maxValue;
						}
						toWhereCondQuery += buildCondition(toFilterColObj, request);
						if (i != toFilterArr.size() - 1) {
							toWhereCondQuery += " AND ";
						}
					}
				}
			}
			if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += Slicecolumn + " ";
				whereCondQuery += "IN";
				whereCondQuery += value;
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
				if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery)) {
					fromWhereCondQuery = whereCondQuery + " AND " + fromWhereCondQuery;
				}
				if (toWhereCondQuery != null && !"".equalsIgnoreCase(toWhereCondQuery)) {
					toWhereCondQuery = whereCondQuery + " AND " + toWhereCondQuery;
				}
			} else {
				if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery)) {
					fromWhereCondQuery = " WHERE " + fromWhereCondQuery;
				}
				if (toWhereCondQuery != null && !"".equalsIgnoreCase(toWhereCondQuery)) {
					toWhereCondQuery = " WHERE " + toWhereCondQuery;
				}
			}
			JSONArray trendDataArr = new JSONArray();
			int randNum = rand.nextInt();
			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {

				long fromCount = 0;
				long toCount = 0;
				long totalCount = 0;
				String percent = "";
				if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery) && toWhereCondQuery != null
						&& !"".equalsIgnoreCase(toWhereCondQuery)) {
					if (fromWhereCondQuery != null && !"".equalsIgnoreCase(fromWhereCondQuery)) {
						String fromQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName
								+ fromWhereCondQuery;
						List fromCountList = access.sqlqueryWithParams(fromQuery, new HashMap());
						if (fromCountList != null && !fromCountList.isEmpty()) {
							fromCount = new PilogUtilities().convertIntoInteger(fromCountList.get(0));
						}
					}
					if (toWhereCondQuery != null && !"".equalsIgnoreCase(toWhereCondQuery)) {
						String toQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + toWhereCondQuery;
						List toCountList = access.sqlqueryWithParams(toQuery, new HashMap());
						if (toCountList != null && !toCountList.isEmpty()) {
							toCount = new PilogUtilities().convertIntoInteger(toCountList.get(0));
						}
					}
					if (type != null && !"".equalsIgnoreCase(type) && !"UniqueCount".equals(type)) {
						selectQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + whereCondQuery;
					} else {
						selectQuery = "SELECT COUNT(*) AS VALUE FROM (SELECT  DISTINCT " + columnName + " FROM "
								+ tableName + whereCondQuery + ")";
					}

					List countList = access.sqlqueryWithParams(selectQuery, new HashMap());
					if (countList != null && !countList.isEmpty()) {
						totalCount = new PilogUtilities().convertIntoInteger(countList.get(0));
					}
					trendDataArr.add(fromCount);
					trendDataArr.add(toCount);
					if (fromCount > toCount) {
						long diff = (fromCount - toCount);
						double percentage = (diff * 100) / fromCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/thumbsdown.png' class='icon' style='width: 30px;'></span>";
					} else if (toCount > fromCount) {
						long diff = (toCount - fromCount);
						double percentage = (diff * 100) / toCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/like.png' class='icon' style='width: 30px;'></span>";
					}
				} else if (dateColumnName != null && !"".equalsIgnoreCase(dateColumnName)) {
					fromWhereCondQuery = "TO_DATE(TO_CHAR(" + dateColumnName
							+ ",'YYYY-MM-DD'), 'YYYY-MM-DD') BETWEEN TO_DATE('" + earlierMonth
							+ "','YYYY-MM-DD') AND TO_DATE('" + earlierDay + "','YYYY-MM-DD')";
					toWhereCondQuery = "TO_DATE(TO_CHAR(" + dateColumnName
							+ ",'YYYY-MM-DD'), 'YYYY-MM-DD') BETWEEN TO_DATE('" + earlier
							+ "','YYYY-MM-DD') AND TO_DATE('" + now + "','YYYY-MM-DD')";
					if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)) {
						fromWhereCondQuery = whereCondQuery + " AND " + fromWhereCondQuery;
						toWhereCondQuery = whereCondQuery + " AND " + toWhereCondQuery;
					} else {
						fromWhereCondQuery = " WHERE " + fromWhereCondQuery;
						toWhereCondQuery = " WHERE " + toWhereCondQuery;
					}
					String fromQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + fromWhereCondQuery;
					List fromCountList = access.sqlqueryWithParams(fromQuery, new HashMap());
					if (fromCountList != null && !fromCountList.isEmpty()) {
						fromCount = new PilogUtilities().convertIntoInteger(fromCountList.get(0));
					}
					String toQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + toWhereCondQuery;
					List toCountList = access.sqlqueryWithParams(toQuery, new HashMap());
					if (toCountList != null && !toCountList.isEmpty()) {
						toCount = new PilogUtilities().convertIntoInteger(toCountList.get(0));
					}
					if (type != null && !"".equalsIgnoreCase(type) && !"UniqueCount".equals(type)) {
						selectQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + whereCondQuery;
					} else {
						selectQuery = "SELECT COUNT(*) AS VALUE FROM (SELECT  DISTINCT " + columnName + " FROM "
								+ tableName + whereCondQuery + ")";
					}

					List countList = access.sqlqueryWithParams(selectQuery, new HashMap());
					if (countList != null && !countList.isEmpty()) {
						totalCount = new PilogUtilities().convertIntoInteger(countList.get(0));
					}
					trendDataArr.add(fromCount);
					trendDataArr.add(toCount);
					if (fromCount > toCount) {
						long diff = (fromCount - toCount);
						double percentage = (diff * 100) / fromCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/thumbsdown.png' class='icon' style='width: 30px;'></span>";
					} else if (toCount > fromCount) {
						long diff = (toCount - fromCount);
						double percentage = (diff * 100) / toCount;
						percent = "<span class='thumbsValueClass'>" + percentage
								+ "%</span><span class='thumbsvalue'> <img src='images/like.png' class='icon' style='width: 30px;'></span>";
					}
				} else {
					if (type != null && !"".equalsIgnoreCase(type) && !"UniqueCount".equals(type)) {
						selectQuery = "SELECT " + type + "(" + columnName + ") FROM " + tableName + whereCondQuery;
					} else {
						selectQuery = "SELECT COUNT(*) AS VALUE FROM (SELECT  DISTINCT " + columnName + " FROM "
								+ tableName + whereCondQuery + ")";
					}

					List countList = access.sqlqueryWithParams(selectQuery, new HashMap());
					if (countList != null && !countList.isEmpty()) {
						totalCount = new PilogUtilities().convertIntoInteger(countList.get(0));
					}
				}

				HashMap datamap = new HashMap();
				if (dashboardCardId != null && !dashboardCardId.isEmpty()) {
					String selectquery = "SELECT CHART_TITTLE, " // 0
							+ "VISUALIZE_CUST_COL17 " // 1
							+ "FROM O_RECORD_VISUALIZATION WHERE ROLE_ID =:ROLE_ID  AND CHART_ID IN('" + dashboardCardId
							+ "')";
					datamap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
					List dashboardCardList = access.sqlqueryWithParams(selectquery, datamap);
					if (dashboardCardList != null && !dashboardCardList.isEmpty()) {
						Object[] cardListArray = (Object[]) dashboardCardList.get(0);
						if (!dashboardutils.isNullOrEmpty(isApplyEvt) && "Y".equalsIgnoreCase(isApplyEvt)) {
							dashboardCardTitle = Lebel;
						} else {
							dashboardCardTitle = String.valueOf(cardListArray[0]);
						}
						if (cardListArray[1] != null) {
							cardImageClob = (Clob) cardListArray[1];
						}
					}
				}

				if (cardImageClob != null) {
					cardImageEncodedString = cloudUtills.clobToString(cardImageClob);
					cardImageTag = "<img src=\"" + cardImageEncodedString
							+ "\"class='cardImageHomepage' id=\"\" width='50px'>";
				} else {
					cardImageTag = "<img src='images/pricebasket1.png' class='cardImageHomepage' id=\"\" width='50px'>";
				}

				if (totalCount >= 0) {
					String datacount = withSuffix(totalCount);
                     
					if(cardTrend !=null && !"".equalsIgnoreCase(cardTrend) && "Trend".equalsIgnoreCase(cardTrend))
					{
						if (cardType != null && !"".equalsIgnoreCase(cardType) && "Rectangle".equalsIgnoreCase(cardType)) {

	                          result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesHome'>"
									+ "<img src=\"images/Horizontal_Dots.svg\" title=\"Edit card\" class=\"homepageCardEdit\" "
									+ "onclick = showEditCardMenu(this,'" + dashboardCardId + "','cardEditEvt')>"
									+ "<div class='titleMainClass'>" + "<span class=\"visionVisualizeCardLevelSpanClass\">"
									+ dashboardCardTitle + "</span>" + "<span id='Cardtype' style='display: none;'>" + type
									+ "  </span>" + "</div>" + "<div class='mainCardClassHome'>"
									+ "<div class='align-self-center'>"
//									+ " <img src='images/pricebasket1.png' class='icon' style='width:30px; margin-left: 10px;'>"
									+ cardImageTag + "</div>" + "<div class='visionVisualizeCardValuesSpanClass'>"
									+ datacount + "</div>" + "<div id='visionVisualizeCardTrendGraph" + randNum
									+ "' class='plotlyImage'>"
									+ " <img src='images/cardGraphImage.png' onclick = \"getCardImageData('"
									+ dashboardCardId + "','')\" class='icon' style='width:30px; margin-right: 10px;'>"
									+ "</div>" + "</div>" + "<div class='thumbsDateClass'>"
									+ "<div class='plotlyRightImage' style='text-align: center;'>" + percent + "</div>"
									+ "</div>" + "<div class='todayDate'>" + "<span class=\"todayDate\">" + date + "</span>"
									+ "</div>" + "</div>";

						} else if (cardType != null && !"".equalsIgnoreCase(cardType)
								&& "Oval".equalsIgnoreCase(cardType)) {
							result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesOvalHome'>"
									+ "<div class='titleMainClass'>" + "<span class=\"visionVisualizeCardLevelSpanClass\">"
									+ dashboardCardTitle + "</span>" + "<span id='Cardtype' style='display: none;'>" + type
									+ "  </span>" + "</div>" + "<div class='mainCardClassHome'>"
									+ "<div class='align-self-center'>"
//									+ " <img src='images/pricebasket1.png' class='icon' style='width:30px; margin-left: 10px;'>"
									+ cardImageTag + "</div>" + "<div class='visionVisualizeCardValuesSpanClass'>"
									+ datacount + "</div>" + "<div id='visionVisualizeCardTrendGraph" + randNum
									+ "' class='plotlyImage'>"
									+ " <img src='images/cardGraphImage.png' onclick = \"getCardImageData('\" +   dashboardCardId + \"','')\" class='icon' style='width:30px; margin-right: 10px;'>"
									+ "</div>" + "</div>" + "<div class='thumbsDateClass'>"
									+ "<div class='plotlyRightImage' style='text-align: center;'>" + percent + "</div>"
									+ "</div>" + "<div class='todayDate'>" + "<span class=\"todayDate\">" + date + "</span>"
									+ "</div>" + "</div>";
						} else if (cardType != null && !"".equalsIgnoreCase(cardType)
								&& "Round".equalsIgnoreCase(cardType)) {
							result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesRoundHome'>"
									+ "<div class='titleMainClass'>" + "<span class=\"visionVisualizeCardLevelSpanClass\">"
									+ dashboardCardTitle + "</span>" + "<span id='Cardtype' style='display: none;'>" + type
									+ "  </span>" + "</div>" + "<div class='mainCardClassHome'>"
									+ "<div class='align-self-center'>"
//									+ " <img src='images/pricebasket1.png' class='icon' style='width:30px; margin-left: 10px;'>"
									+ cardImageTag + "</div>" + "<div class='visionVisualizeCardValuesSpanClass'>"
									+ datacount + "</div>" + "<div id='visionVisualizeCardTrendGraph" + randNum
									+ "' class='plotlyImage'>"
									+ " <img src='images/cardGraphImage.png' onclick = \"getCardImageData('\" +   dashboardCardId + \"','')\" class='icon' style='width:30px; margin-right: 10px;'>"
									+ "</div>" + "</div>" + "<div class='thumbsDateClass'>"
									+ "<div class='plotlyRightImage' style='text-align: center;'>" + percent + "</div>"
									+ "</div>" + "<div class='todayDate'>" + "<span class=\"todayDate\">" + date + "</span>"
									+ "</div>" + "</div>";
						} else if (cardType != null && !"".equalsIgnoreCase(cardType)
								&& "Normal".equalsIgnoreCase(cardType)) {
							result += "<div class='visionVisualizeNormalCardHomeClass'>" + "<div class='titleMainClass'>"
									+ "<img src=\"images/Horizontal_Dots.svg\" title=\"Edit card\" class=\"homepageCardEdit hpCardEditBasicImg\" "
									+ "onclick = showEditCardMenu(this,'" + dashboardCardId + "','cardEditEvt')>"
									+ "<span class=\"visionVisualizeCardLevelSpanClass\">" + dashboardCardTitle + "</span>"
									+ "<span id='Cardtype' style='display: none;'>" + type + "  </span>" + "</div>"
									+ "<div class='mainCardClassHome'>" + "<div class='visionVisualizeCardValuesSpanClass'>"
									+ datacount + "</div>" + "</div>" + "</div>";
						}
					}else {

					if (cardType != null && !"".equalsIgnoreCase(cardType) && "Rectangle".equalsIgnoreCase(cardType)) {

                          result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesHome'>"
								+ "<img src=\"images/Horizontal_Dots.svg\" title=\"Edit card\" class=\"homepageCardEdit\" "
								+ "onclick = showEditCardMenu(this,'" + dashboardCardId + "','cardEditEvt')>"
								+ "<div class='titleMainClass'>" + "<span class=\"visionVisualizeCardLevelSpanClass\">"
								+ dashboardCardTitle + "</span>" + "<span id='Cardtype' style='display: none;'>" + type
								+ "  </span>" + "</div>" 
								+ "<div class='mainCardClassHome'>"
								+ "<div class='visionVisualizeCardValuesSpanClass'>"
								+ datacount + "</div>" 
                                + "</div>" 
								+ "</div>";

					} else if (cardType != null && !"".equalsIgnoreCase(cardType)
							&& "Oval".equalsIgnoreCase(cardType)) {
						result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesOvalHome'>"
								+ "<div class='titleMainClass'>" + "<span class=\"visionVisualizeCardLevelSpanClass\">"
								+ dashboardCardTitle + "</span>" + "<span id='Cardtype' style='display: none;'>" + type
								+ "  </span>" + "</div>" 
								+ "<div class='mainCardClassHome'>"
								+ "<div class='visionVisualizeCardValuesSpanClass'>"
								+ datacount + "</div>" 
								+ "</div>" 
								+ "</div>";
					} else if (cardType != null && !"".equalsIgnoreCase(cardType)
							&& "Round".equalsIgnoreCase(cardType)) {
						result += "<div class='visionVisualizeCardValuesDivClass visionVisualizeCardValuesRoundHome'>"
								+ "<div class='titleMainClass'>" + "<span class=\"visionVisualizeCardLevelSpanClass\">"
								+ dashboardCardTitle + "</span>" + "<span id='Cardtype' style='display: none;'>" + type
								+ "  </span>" + "</div>" 
								+ "<div class='mainCardClassHome'>"
								+ "<div class='visionVisualizeCardValuesSpanClass'>"
								+ datacount + "</div>" 
                                + "</div>" 
								+ "</div>";
					} else if (cardType != null && !"".equalsIgnoreCase(cardType)
							&& "Normal".equalsIgnoreCase(cardType)) {
						result += "<div class='visionVisualizeNormalCardHomeClass'>" + "<div class='titleMainClass'>"
								+ "<img src=\"images/Horizontal_Dots.svg\" title=\"Edit card\" class=\"homepageCardEdit hpCardEditBasicImg\" "
								+ "onclick = showEditCardMenu(this,'" + dashboardCardId + "','cardEditEvt')>"
								+ "<span class=\"visionVisualizeCardLevelSpanClass\">" + dashboardCardTitle + "</span>"
								+ "<span id='Cardtype' style='display: none;'>" + type + "  </span>" + "</div>"
								+ "<div class='mainCardClassHome'>" + "<div class='visionVisualizeCardValuesSpanClass'>"
								+ datacount + "</div>" + "</div>" + "</div>";
					}
					}

				}
			}
			dataObj.put("result", result);
			dataObj.put("trendDataArr", trendDataArr);
			dataObj.put("trendLabelsArr", trendLabelsArr);
			dataObj.put("cardTrendType", cardTrendType);
			dataObj.put("randNum", randNum);
			dataObj.put("dateColumnName", dateColumnName);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	@Transactional
	public JSONObject fetchpredictiveChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			String selectQuery = "";
			String whereCondQuery = "";
			String groupByCond = "";
			String groupBy = "";
			List<String> columnKeys = new ArrayList<>();
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String aggregateColumns = request.getParameter("aggregateColumns");
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String tables = request.getParameter("tablesObj");
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject chartConfigObj = new JSONObject();

			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (aggregateColumns != null && !"".equalsIgnoreCase(aggregateColumns)
					&& !"null".equalsIgnoreCase(aggregateColumns)) {
				aggregateColsArr = (JSONArray) JSONValue.parse(aggregateColumns);
			}

			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
//                    if (axisColObj != null && !axisColObj.isEmpty()) {
//                        selectQuery += " " + axisColObj.get("columnName") + " ,";
//                    }
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						String[] filteredColumnnameArr = columnName.split("\\.");
						String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\p{P}", "");
						columnKeys.add(filteredColumnname);
						selectQuery += " " + columnName + ", ";
					}
				}
			}
			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String[] filteredColumnnameArr = columnName.split("\\.");
						String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\p{P}", "");
						columnKeys.add(filteredColumnname);
						if (aggregateColsArr != null && !aggregateColsArr.isEmpty() && aggregateColsArr.size() > 0) {
							selectQuery += " " + columnName + " AS COL" + i + " ,";
						} else {
							selectQuery += " " + columnName + ", ";
						}
					}
				}
			}
			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}

			if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue)) {
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += Slicecolumn + " ";
				whereCondQuery += "IN";
				whereCondQuery += value;
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			JSONObject configObj = buildOptionsObj(request, chartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			System.out.println("LayoutObj :::" + layoutObj);
			System.out.println("DataPropObj :::" + dataPropObj);
			JSONArray colorsArr = new JSONArray();
			JSONArray markerColorsArr = new JSONArray();
			if (dataPropObj != null && !dataPropObj.isEmpty()) {
				JSONObject markerObj = (JSONObject) dataPropObj.get("marker");
				if (markerObj != null && !markerObj.isEmpty()) {
					if (markerObj.get("colors") instanceof JSONArray) {
						colorsArr = (JSONArray) markerObj.get("colors");
					} else {
						String colorValues = (String) markerObj.get("colors");
						if (colorValues != null && !"".equalsIgnoreCase(colorValues)
								&& !"null".equalsIgnoreCase(colorValues)) {
							colorsArr.add(colorValues);
						}
					}

				}
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			JSONObject dataObj = new JSONObject();
			if (selectQuery != null && !"".equalsIgnoreCase(selectQuery) && tablesArr != null && !tablesArr.isEmpty()) {
				String tableName = (String) tablesArr.get(0);
				selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
				selectQuery = "SELECT " + selectQuery + " FROM " + tableName + whereCondQuery + groupByCond;
				System.out.println("selectQuery :::" + selectQuery);
				List selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					int c = 0;
					for (int i = 0; i < selectData.size(); i++) {
						Object[] rowData = (Object[]) selectData.get(i);
						for (int j = 0; j < rowData.length; j++) {
							if (dataObj != null && !dataObj.isEmpty() && dataObj.get(columnKeys.get(j)) != null) {
								JSONArray jsonDataArr = (JSONArray) dataObj.get(columnKeys.get(j));
								jsonDataArr.add(rowData[j]);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							} else {
								JSONArray jsonDataArr = new JSONArray();
								jsonDataArr.add(rowData[j]);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							}
						}

						if (colorsArr != null && !colorsArr.isEmpty()) {
							if (c > colorsArr.size() - 1) {
								c = 0;
							}
							markerColorsArr.add(colorsArr.get(c));
						}
						c++;
					}

				}
			}
			if (layoutObj != null && !layoutObj.isEmpty()) {
				JSONObject markerObj = (JSONObject) layoutObj.get("marker");
				if (markerObj != null && !markerObj.isEmpty() && markerColorsArr != null
						&& !markerColorsArr.isEmpty()) {
					markerObj.put("colors", markerColorsArr);
				}
			}
			chartObj.put("dataPropObject", dataPropObj);
			chartObj.put("data", dataObj);
			chartObj.put("layout", layoutObj);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public String withSuffix(long count) {
		if (count < 1000) {
			return "" + count;
		}
		int exp = (int) (Math.log(count) / Math.log(1000));
		return String.format("%.1f %c", count / Math.pow(1000, exp), "KMGTPE".charAt(exp - 1));
	}
	
	
	public String saveVisualizeData(HttpServletRequest request) {
		String result = "";
		int count = 0;
		Random rand = new Random();
		String data = request.getParameter("data");
		String userName = (String) request.getSession(false).getAttribute("ssUsername");
		String filepath = fileStoreHomedirectory + "images/" + userName;
		File cardImageFileDirectory = new File(filepath);
		if (data != null && !"".equalsIgnoreCase(data) && !"null".equalsIgnoreCase(data)) {
			JSONArray dataArr = (JSONArray) JSONValue.parse(data);
			if (dataArr != null && !dataArr.isEmpty()) {
				for (int k = 0; k < dataArr.size(); k++) {
					JSONObject dataObj = (JSONObject) dataArr.get(k);
					if (dataObj != null && !dataObj.isEmpty()) {
						String chartId = (String) dataObj.get("chartId");
						String chartType = (String) dataObj.get("chartType");
						String axisColumns = (String) dataObj.get("axisColumns");
						String valuesColumns = (String) dataObj.get("valuesColumns");
						String comboColumns = (String) dataObj.get("comboColumns");
						String percentColumns = (String) dataObj.get("percentColumns");
						String tables = (String) dataObj.get("tablesObj");
						String filterConditions = (String) dataObj.get("filterConditions");
						String dashBoardName = request.getParameter("dashBoardName");
						String colorsObj = (String) dataObj.get("colorsObj");
						String chartPropObj = (String) dataObj.get("chartPropObj");
						String chartConfigPositionKeyStr = (String) dataObj.get("chartConfigPositionKeyStr");
						String chartConfigToggleStatus = (String) dataObj.get("chartConfigToggleStatus");
						String currencyConversionStrObject = (String) dataObj.get("currencyConversionObject");
						String paramFromStr = (String) dataObj.get("paramFromArr");
						String paramToStr = (String) dataObj.get("paramToArr");
						String paramDateArr = (String) dataObj.get("paramDateArr");
						String cardType = (String) dataObj.get("cardType");
						String cardTrendsChartType = (String) dataObj.get("cardTrendsChartType");
						String cardTrend = (String) dataObj.get("cardTrend");
						String cardTitle = (String) dataObj.get("cardTitle");
						String cardImageEncodedStr = (String) dataObj.get("cardImageEncodedStr");
						JSONObject paramCardDateObj = new JSONObject();
						if (paramFromStr != null && !"".equalsIgnoreCase(paramFromStr) && paramToStr != null
								&& !"".equalsIgnoreCase(paramToStr)) {
							paramCardDateObj.put("paramFromArr", paramFromStr);
							paramCardDateObj.put("paramToArr", paramToStr);
						} else if (paramDateArr != null && !"".equalsIgnoreCase(paramDateArr)) {
							paramCardDateObj.put("paramDateArr", paramDateArr);
						}
						String cardColumnLabelTitleCase = "";
						JSONArray axisColsArr = new JSONArray();
						JSONArray valuesColsArr = new JSONArray();
						JSONArray comboColsArr= new JSONArray();
						JSONArray percentColsArr = new JSONArray();
						JSONArray tablesArr = new JSONArray();

						if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
							tablesArr = (JSONArray) JSONValue.parse(tables);
						}
						if (axisColumns != null && !"".equalsIgnoreCase(axisColumns)
								&& !"null".equalsIgnoreCase(axisColumns)) {
							axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
						}
						if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
								&& !"null".equalsIgnoreCase(valuesColumns)) {
							valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
						}
						if (comboColumns != null && !"".equalsIgnoreCase(comboColumns)
								&& !"null".equalsIgnoreCase(comboColumns)) {
							comboColsArr = (JSONArray) JSONValue.parse(comboColumns);
						}
						if (percentColumns != null && !"".equalsIgnoreCase(percentColumns)
								&& !"null".equalsIgnoreCase(percentColumns)) {
							percentColsArr = (JSONArray) JSONValue.parse(percentColumns);
						}

						if (chartId != null && !"".equalsIgnoreCase(chartId)) {
							chartId = chartId + "_" + rand.nextInt();
						}
						try {
							if (chartType != null && !"".equalsIgnoreCase(chartType)) {
								String insertQuery = "INSERT INTO O_RECORD_VISUALIZATION(" + "ORGN_ID, " // 1
										+ " ROLE_ID, " // 2
										+ " X_AXIS_VALUE, " // 3
										+ " Y_AXIS_VALUE, " //4
										+ " COMBO_VALUE, "// 5
										+ " CHART_TYPE, " // 6
										+ " TABLE_NAME, " // 7
										+ " CHART_ID, " // 8
										+ " AGGRIGATE_COLUMNS, " // 9
										+ " CLOB_CHARTPROP, " // 10
										+ " CLOB_CHARTDATA, " // 11
										+ " CLOB_CHARTLAYOUT, " // 12
										+ " FILTER_CONDITION, " // 13
										+ " CHART_PROPERTIES, " // 14
										+ " CHART_CONFIG_OBJECT, " // 15
										+ " CREATE_BY, " // 16
										+ "FILTER_COLUMN, "// 17
										+ " EDIT_BY, " // 18
										+ "DASHBORD_NAME, "// 19
										+ "CHART_TITTLE, "// 20
										+ "VISUALIZE_CUST_COL8,"// 21
										+ "VISUALIZE_CUST_COL9," // 22
										+ "VISUALIZE_CUST_COL13," // 23
										+ "VISUALIZE_CUST_COL14," // 24
										+ "VISUALIZE_CUST_COL18," // 25
										+ "VISUALIZE_CUST_COL15, " // 26
										+ "VISUALIZE_CUST_COL17," //27
										+ "VISUALIZE_CUST_COL12," // 28
										+ "Z_AXIS_VALUE)"//29
										+ " Values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
								Map<Integer, Object> insertMap = new HashMap<>();
								insertMap.put(1, request.getSession(false).getAttribute("ssOrgId"));
								insertMap.put(2, request.getSession(false).getAttribute("ssRole"));
								insertMap.put(3, axisColsArr.toString());
								insertMap.put(4, valuesColsArr.toString());
								insertMap.put(5, comboColsArr.toString());
								insertMap.put(6, chartType);
								insertMap.put(7, tablesArr.get(0));
								insertMap.put(8, chartId);
								insertMap.put(9, "");
								insertMap.put(10, null);
								insertMap.put(11, null);
								insertMap.put(12, null);
								insertMap.put(13, filterConditions);
								insertMap.put(14, chartPropObj);
								insertMap.put(15, chartConfigPositionKeyStr);
								insertMap.put(16, request.getSession(false).getAttribute("ssUsername"));
								insertMap.put(17, null);
								insertMap.put(18, request.getSession(false).getAttribute("ssUsername"));
								insertMap.put(19, dashBoardName);
								insertMap.put(20, cardTitle);
								insertMap.put(21, colorsObj);
								insertMap.put(22, chartConfigToggleStatus);
								insertMap.put(23, cardType);
								insertMap.put(24, paramCardDateObj.toJSONString());
								insertMap.put(25, cardTrendsChartType);
								insertMap.put(26, currencyConversionStrObject);
								insertMap.put(27, cardImageEncodedStr);
								insertMap.put(28, cardTrend);
								insertMap.put(29, percentColsArr.toString());
								System.out.println("insertQuery::::::" + insertQuery);
								System.out.println("insertMap::::::" + insertMap);
								count = access.executeNativeUpdateSQLWithSimpleParamsNoAudit(insertQuery, insertMap);
								if (count != 0) {
									result = "Published successFully";
									FileUtils.deleteDirectory(cardImageFileDirectory);
									cardImageFileDirectory.delete();
								} else {
									result = "Failed to insert";
								}

							} else {
								result = "Failed to insert";
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} 
		return result;

	}

	/*
	 * @Transactional public String saveVisualizeData(HttpServletRequest request) {
	 * String result = ""; int count = 0; Random rand = new Random(); String data =
	 * request.getParameter("data"); String userName = (String)
	 * request.getSession(false).getAttribute("ssUsername"); String filepath =
	 * fileStoreHomedirectory + "images/" + userName; File cardImageFileDirectory =
	 * new File(filepath); if (data != null && !"".equalsIgnoreCase(data) &&
	 * !"null".equalsIgnoreCase(data)) { JSONArray dataArr = (JSONArray)
	 * JSONValue.parse(data); if (dataArr != null && !dataArr.isEmpty()) { for (int
	 * k = 0; k < dataArr.size(); k++) { JSONObject dataObj = (JSONObject)
	 * dataArr.get(k); if (dataObj != null && !dataObj.isEmpty()) { String chartId =
	 * (String) dataObj.get("chartId"); String chartType = (String)
	 * dataObj.get("chartType"); String axisColumns = (String)
	 * dataObj.get("axisColumns"); String valuesColumns = (String)
	 * dataObj.get("valuesColumns"); String tables = (String)
	 * dataObj.get("tablesObj"); String filterConditions = (String)
	 * dataObj.get("filterConditions"); String dashBoardName =
	 * request.getParameter("dashBoardName"); String colorsObj = (String)
	 * dataObj.get("colorsObj"); String chartPropObj = (String)
	 * dataObj.get("chartPropObj"); String chartConfigPositionKeyStr = (String)
	 * dataObj.get("chartConfigPositionKeyStr"); String chartConfigToggleStatus =
	 * (String) dataObj.get("chartConfigToggleStatus"); String
	 * currencyConversionStrObject = (String)
	 * dataObj.get("currencyConversionObject"); String paramFromStr = (String)
	 * dataObj.get("paramFromArr"); String paramToStr = (String)
	 * dataObj.get("paramToArr"); String paramDateArr = (String)
	 * dataObj.get("paramDateArr"); String cardType = (String)
	 * dataObj.get("cardType"); String cardTrendsChartType = (String)
	 * dataObj.get("cardTrendsChartType"); String cardTrend = (String)
	 * dataObj.get("cardTrend"); String cardTitle = (String)
	 * dataObj.get("cardTitle"); String cardImageEncodedStr = (String)
	 * dataObj.get("cardImageEncodedStr"); JSONObject paramCardDateObj = new
	 * JSONObject(); if (paramFromStr != null && !"".equalsIgnoreCase(paramFromStr)
	 * && paramToStr != null && !"".equalsIgnoreCase(paramToStr)) {
	 * paramCardDateObj.put("paramFromArr", paramFromStr);
	 * paramCardDateObj.put("paramToArr", paramToStr); } else if (paramDateArr !=
	 * null && !"".equalsIgnoreCase(paramDateArr)) {
	 * paramCardDateObj.put("paramDateArr", paramDateArr); } String
	 * cardColumnLabelTitleCase = ""; JSONArray axisColsArr = new JSONArray();
	 * JSONArray valuesColsArr = new JSONArray(); JSONArray tablesArr = new
	 * JSONArray();
	 * 
	 * if (tables != null && !"".equalsIgnoreCase(tables) &&
	 * !"null".equalsIgnoreCase(tables)) { tablesArr = (JSONArray)
	 * JSONValue.parse(tables); } if (axisColumns != null &&
	 * !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
	 * axisColsArr = (JSONArray) JSONValue.parse(axisColumns); } if (valuesColumns
	 * != null && !"".equalsIgnoreCase(valuesColumns) &&
	 * !"null".equalsIgnoreCase(valuesColumns)) { valuesColsArr = (JSONArray)
	 * JSONValue.parse(valuesColumns); }
	 * 
	 * if (chartId != null && !"".equalsIgnoreCase(chartId)) { chartId = chartId +
	 * "_" + rand.nextInt(); } try { if (chartType != null &&
	 * !"".equalsIgnoreCase(chartType)) { String insertQuery =
	 * "INSERT INTO O_RECORD_VISUALIZATION(" + "ORGN_ID, " // 1 + " ROLE_ID, " // 2
	 * + " X_AXIS_VALUE, " // 3 + " Y_AXIS_VALUE, " // 4 + " CHART_TYPE, " // 5 +
	 * " TABLE_NAME, " // 6 + " CHART_ID, " // 7 + " AGGRIGATE_COLUMNS, " // 8 +
	 * " CLOB_CHARTPROP, " // 9 + " CLOB_CHARTDATA, " // 10 + " CLOB_CHARTLAYOUT, "
	 * // 11 + " FILTER_CONDITION, " // 12 + " CHART_PROPERTIES, " // 13 +
	 * " CHART_CONFIG_OBJECT, " // 14 + " CREATE_BY, " // 15 + "FILTER_COLUMN, "//
	 * 16 + " EDIT_BY, " // 17 + "DASHBORD_NAME, "// 18 + "CHART_TITTLE, "// 19 +
	 * "VISUALIZE_CUST_COL8,"// 20 + "VISUALIZE_CUST_COL9," // 21 +
	 * "VISUALIZE_CUST_COL13," // 22 + "VISUALIZE_CUST_COL14," // 23 +
	 * "VISUALIZE_CUST_COL18," // 24 + "VISUALIZE_CUST_COL15, " // 25 +
	 * "VISUALIZE_CUST_COL17," //26 + "VISUALIZE_CUST_COL12)" // 27 +
	 * " Values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	 * Map<Integer, Object> insertMap = new HashMap<>(); insertMap.put(1,
	 * request.getSession(false).getAttribute("ssOrgId")); insertMap.put(2,
	 * request.getSession(false).getAttribute("ssRole")); insertMap.put(3,
	 * axisColsArr.toString()); insertMap.put(4, valuesColsArr.toString());
	 * insertMap.put(5, chartType); insertMap.put(6, tablesArr.get(0));
	 * insertMap.put(7, chartId); insertMap.put(8, ""); insertMap.put(9, null);
	 * insertMap.put(10, null); insertMap.put(11, null); insertMap.put(12,
	 * filterConditions); insertMap.put(13, chartPropObj); insertMap.put(14,
	 * chartConfigPositionKeyStr); insertMap.put(15,
	 * request.getSession(false).getAttribute("ssUsername")); insertMap.put(16,
	 * null); insertMap.put(17,
	 * request.getSession(false).getAttribute("ssUsername")); insertMap.put(18,
	 * dashBoardName); insertMap.put(19, cardTitle); insertMap.put(20, colorsObj);
	 * insertMap.put(21, chartConfigToggleStatus); insertMap.put(22, cardType);
	 * insertMap.put(23, paramCardDateObj.toJSONString()); insertMap.put(24,
	 * cardTrendsChartType); insertMap.put(25, currencyConversionStrObject);
	 * insertMap.put(26, cardImageEncodedStr); insertMap.put(27, cardTrend);
	 * System.out.println("insertQuery::::::" + insertQuery);
	 * System.out.println("insertMap::::::" + insertMap); count =
	 * access.executeNativeUpdateSQLWithSimpleParamsNoAudit(insertQuery, insertMap);
	 * if (count != 0) { result = "Published successFully";
	 * FileUtils.deleteDirectory(cardImageFileDirectory);
	 * cardImageFileDirectory.delete(); } else { result = "Failed to insert"; }
	 * 
	 * } else { result = "Failed to insert"; }
	 * 
	 * } catch (Exception e) { e.printStackTrace(); } } } } } return result;
	 * 
	 * }
	 */

//    @Transactional
//    public JSONObject getChartData(HttpServletRequest request) {
//        JSONObject tabledataobj = new JSONObject();
//        JSONArray dataarr = new JSONArray();
//        try {
//            String userName = (String) request.getSession(false).getAttribute("ssUsername");
//            String selectquery = "SELECT X_AXIX, Y_AXIX,CHART_TYPE,TABLE_NAME,CHART_ID FROM O_RECORD_VISUALIZATION WHERE USER_NAME =:USER_NAME";
//            HashMap datamap = new HashMap();
//            datamap.put("USER_NAME", userName);
//            List datalist = access.sqlqueryWithParams(selectquery, datamap);
//            if (datalist != null && !datalist.isEmpty()) {
//                for (int i = 0; i < datalist.size(); i++) {
//                    Object[] rowData = (Object[]) datalist.get(i);
//                    JSONObject dataobj = new JSONObject();
//                    dataobj.put("xAxix", rowData[0]);
//                    dataobj.put("yAxix", rowData[1]);
//                    dataobj.put("type", rowData[2]);
//                    dataobj.put("table", rowData[3]);
//                    dataobj.put("chartid", rowData[4]);
//                    dataarr.add(dataobj);
//                }
//                tabledataobj.put("dataarr", dataarr);
//            }
//            String div = "<div id ='visualizechart' class ='visualizechartclass'></div>";
//            tabledataobj.put("chartDiv", div);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return tabledataobj;
//    }
	@Transactional
	public JSONObject getChartData(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String dashbordname = request.getParameter("dashbordname");
			String chartIds = request.getParameter("chartIds");
			String roleId = (String) request.getSession(false).getAttribute("ssRole");
			String subQuery = "";
			HashMap dashBoardMap = new HashMap();
			if (roleId != null && !"".equalsIgnoreCase(roleId) && !"".equalsIgnoreCase(roleId)
					&& (roleId.contains("REQUESTOR") || roleId.contains("APPROVER"))) {
				subQuery = " AND CREATE_BY =:CREATE_BY ";
				dashBoardMap.put("CREATE_BY", userName);
			}
			String dashBoardQuery = "SELECT DISTINCT DASHBORD_NAME FROM O_RECORD_VISUALIZATION WHERE ROLE_ID =:ROLE_ID "
					+ subQuery + " ORDER BY DASHBORD_NAME";
			JSONArray dashBordArr = new JSONArray();
			dashBoardMap.put("ROLE_ID", roleId);
			List dashBoardList = access.sqlqueryWithParams(dashBoardQuery, dashBoardMap);
			if (dashBoardList != null && !dashBoardList.isEmpty()) {
				for (int i = 0; i < dashBoardList.size(); i++) {
					if (!(dashbordname != null && !"".equalsIgnoreCase(dashbordname)
							&& !"null".equalsIgnoreCase(dashbordname))) {
						dashbordname = (String) dashBoardList.get(i);
					}
					dashBordArr.add(dashBoardList.get(i));
				}
			}
			String kanbanViewName = request.getParameter("kanbanViewName");
			String kanbanViewQuery = "SELECT DISTINCT CHART_ID FROM O_RECORD_VISUALIZATION WHERE CREATE_BY =:CREATE_BY AND CHART_TYPE=:CHART_TYPE";
			String kanbanViewNameStr = "";
			HashMap kanbanViewNameMap = new HashMap();
			kanbanViewNameMap.put("CREATE_BY", userName);
			kanbanViewNameMap.put("CHART_TYPE", "KANBAN");
			List kanbanViewList = access.sqlqueryWithParams(kanbanViewQuery, kanbanViewNameMap);
			if (kanbanViewList != null && !kanbanViewList.isEmpty()) {
				String selected = "";
				kanbanViewNameStr = "<select id ='kanbanOptionListId' class='kanbanOptionListClass' onChange=\"getHomeDashboardKanbanView(event,id)\">";
				kanbanViewNameStr += "<option value= 'Select' selected>Select</option>";
				for (int i = 0; i < kanbanViewList.size(); i++) {
					selected = "";
					if (!(kanbanViewName != null && !"".equalsIgnoreCase(kanbanViewName)
							&& !"null".equalsIgnoreCase(kanbanViewName))) {
						kanbanViewName = (String) kanbanViewList.get(i);
					} else if (kanbanViewName != null && !"".equalsIgnoreCase(kanbanViewName)
							&& (kanbanViewName.trim()).equalsIgnoreCase((String) kanbanViewList.get(i))) {
						selected = "selected";
					}
					kanbanViewNameStr += "<option value= '" + kanbanViewList.get(i) + "' " + selected + ">"
							+ kanbanViewList.get(i) + "</option>";
				}
				kanbanViewNameStr += "</select>";

			}
			HashMap datamap = new HashMap();
			String selectquery = "SELECT " + "X_AXIS_VALUE, "// 0
					+ "Y_AXIS_VALUE,"// 1
					+ "CHART_TYPE,"// 2
					+ "TABLE_NAME,"// 3
					+ "CHART_ID,"// 4
					+ "AGGRIGATE_COLUMNS, "// 5
					+ "FILTER_CONDITION, "// 6
					+ "CHART_PROPERTIES, "// 7
					+ "CHART_CONFIG_OBJECT, "// 8
					+ "VISUALIZE_CUST_COL10, "// 9
					+ "CHART_TITTLE, " // 10
					+ "VISUALIZE_CUST_COL8, " // 11
					+ "VISUALIZE_CUST_COL9, " // 12
					+ "VISUALIZE_CUST_COL5, " // 13
					+ "FILTER_COLUMN, " // 14
					+ "VISUALIZE_CUST_COL6, " // 15
					+ "VISUALIZE_CUST_COL7, " // 16
					+ "COMBO_VALUE, " // 17
					+ "VISUALIZE_CUST_COL15, " // 18
					+ "VISUALIZE_CUST_COL14, " // 19
					+ "VISUALIZE_CUST_COL13, " // 20
					+ "VISUALIZE_CUST_COL18," // 21
					+ "VISUALIZE_CUST_COL12, " // 22
					+ "Z_AXIS_VALUE "//23
					+ "FROM " + "O_RECORD_VISUALIZATION " + "WHERE ROLE_ID =:ROLE_ID ";
			if (chartIds != null && !"".equalsIgnoreCase(chartIds) && !"null".equalsIgnoreCase(chartIds)) {
				JSONArray chartsArr = (JSONArray) JSONValue.parse(chartIds);
				List chartList = new ArrayList(chartsArr);
				chartIds = (String) chartList.stream().collect(Collectors.joining("','", "'", "'"));
				selectquery += " AND CHART_ID IN(" + chartIds + ")";
			} else {
				selectquery += "AND DASHBORD_NAME =:DASHBORD_NAME ";
				datamap.put("DASHBORD_NAME", dashbordname);
			}
			selectquery += "ORDER BY CHART_SEQUENCE_NO";
			datamap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));

			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					JSONObject dataobj = new JSONObject();
					dataobj.put("xAxix", rowData[0]);
					dataobj.put("yAxix", rowData[1]);
					dataobj.put("type", rowData[2]);
					dataobj.put("table", rowData[3]);
					dataobj.put("chartid", rowData[4]);
					dataobj.put("aggColumnName", rowData[5]);
					dataobj.put("filterCondition", rowData[6]);
					dataobj.put("chartPropObj", rowData[7]);
					dataobj.put("chartConfigObj", rowData[8]);
					dataobj.put("labelLegend", rowData[9]);
					dataobj.put("Lebel", rowData[10]);
					dataobj.put("colorsObj", rowData[11]);
					dataobj.put("chartConfigToggleStatus", rowData[12]);
					dataobj.put("compareChartsFlag", rowData[13]);
					dataobj.put("homeFilterColumn", rowData[14]);
					dataobj.put("fetchQuery", rowData[15]);
					dataobj.put("radioButtons", rowData[16]);
					dataobj.put("comboValue", rowData[17]);
					dataobj.put("currencyConversionStrObject", rowData[18]);
					dataobj.put("paramCardDateObj", rowData[19]);
					dataobj.put("cardType", rowData[20]);
					dataobj.put("cardTrendType", rowData[21]);
					dataobj.put("cardTrend", rowData[22]);
					dataobj.put("zAxis",rowData[23]);
					dataarr.add(dataobj);
				}
				tabledataobj.put("dataarr", dataarr);
			}

			tabledataobj.put("dashBordlist", dashBordArr);
			tabledataobj.put("dashBordName", dashbordname);
			tabledataobj.put("kanbanViewNameList", kanbanViewNameStr);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public JSONObject getcharttableattr(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String chartType = request.getParameter("type");
			String chartid = request.getParameter("id");

			String selectquery = "SELECT " + "X_AXIS_VALUE, "// 0
					+ "Y_AXIS_VALUE,"// 1
					+ "CHART_TYPE,"// 2
					+ "TABLE_NAME,"// 3
					+ "CHART_ID,"// 4
					+ "AGGRIGATE_COLUMNS, "// 5
					+ "FILTER_CONDITION, "// 6
					+ "CHART_PROPERTIES, "// 7
					+ "CHART_CONFIG_OBJECT, "// 8
					+ "VISUALIZE_CUST_COL10, "// 9
					+ "CHART_TITTLE, " // 10
					+ "VISUALIZE_CUST_COL8, " // 11
					+ "VISUALIZE_CUST_COL9, " // 12
					+ "VISUALIZE_CUST_COL5, " // 13
					+ "FILTER_COLUMN, " // 14
					+ "VISUALIZE_CUST_COL6, " // 15
					+ "VISUALIZE_CUST_COL7, " // 16
					+ "COMBO_VALUE, " // 17
					+ "VISUALIZE_CUST_COL15 " // 18
					+ "FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID AND CHART_TYPE =:CHART_TYPE";
			HashMap datamap = new HashMap();
			datamap.put("CHART_ID", chartid);
			datamap.put("CHART_TYPE", chartType);
			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					JSONObject dataobj = new JSONObject();
					dataobj.put("xAxix", rowData[0]);
					dataobj.put("yAxix", rowData[1]);
					dataobj.put("type", rowData[2]);
					dataobj.put("table", rowData[3]);
					dataobj.put("chartid", rowData[4]);
					dataobj.put("aggColumnName", rowData[5]);
					dataobj.put("filterCondition", rowData[6]);
					dataobj.put("chartPropObj", rowData[7]);
					dataobj.put("chartConfigObj", rowData[8]); // positions
					dataobj.put("labelLegend", rowData[9]);
					dataobj.put("Lebel", rowData[10]);
					dataobj.put("colorsObj", rowData[11]);
					dataobj.put("chartConfigToggleStatus", rowData[12]);
					dataobj.put("compareChartsFlag", rowData[13]);
					dataobj.put("homeFilterColumn", rowData[14]);
					dataobj.put("fetchQuery", rowData[15]);
					dataobj.put("radioButtons", rowData[16]);
					dataobj.put("comboValue", rowData[17]);
					dataobj.put("currencyConversionStrObject", rowData[18]);
					dataarr.add(dataobj);
				}
				tabledataobj.put("dataarr", dataarr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public String deleteVisualizeChart(HttpServletRequest request) {
		Integer deleteCount = 0;
		String deleteResult = "";
		try {
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String tableName = request.getParameter("tableName");
			String deleteQuery = "DELETE FROM O_RECORD_VISUALIZATION WHERE CHART_ID=:CHART_ID"
					+ " AND CHART_TYPE=:CHART_TYPE";
			Map<String, Object> deleteMap = new HashMap<>();
			deleteMap.put("CHART_ID", chartId);
			deleteMap.put("CHART_TYPE", chartType);
			System.out.println("deleteQuery:::" + deleteQuery);
			System.out.println("deleteMap:::" + deleteMap);
			deleteCount = access.executeUpdateSQLNoAudit(deleteQuery, deleteMap);
			if (deleteCount != null && deleteCount > 0) {
				deleteResult = "Deleted Successfully.";
			} else {
				deleteResult = "Failed to Delete.";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deleteResult;

	}

	@Transactional
	public JSONObject getGrid(String gridId, HttpServletRequest request) {
		JSONObject gridObject = new JSONObject();
		try {
			String ssRole = request.getParameter("ssRole");
			if (!(ssRole != null && !"".equalsIgnoreCase(ssRole) && "Y".equalsIgnoreCase(ssRole))) {
				ssRole = "MM_MANAGER";
			}
			String ssOrgId = request.getParameter("ssOrgId");
			if (!(ssOrgId != null && !"".equalsIgnoreCase(ssOrgId) && "Y".equalsIgnoreCase(ssOrgId))) {
				ssOrgId = "C1F5CFB03F2E444DAE78ECCEAD80D27D";
			}
			JSONObject gridListObj = new JSONObject();
			gridListObj.put(gridId, getGridList(gridId, ssRole, ssOrgId));
			if (gridListObj != null && !gridListObj.isEmpty()) {
				List gridList = (List) gridListObj.get(gridId);
				gridObject = getGrid(gridId, request, gridList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gridObject;
	}

	@Transactional
	public JSONObject getGrid(String gridId, HttpServletRequest request, List gridList) {
		String ssRole = (String) request.getSession(false).getAttribute("ssRole");
		String ssOrgId = (String) request.getSession(false).getAttribute("ssOrgId");
		JSONObject gridObject = new JSONObject();
		JSONObject dropDownListDataObj = new JSONObject();
		JSONObject dependencyDataObj = new JSONObject();
		JSONObject searchButtonObj = new JSONObject();
		boolean tbDdwEditFlag = false;
		String persInd = "N";
		try {
			JSONObject labelsObj = new JSONObject();

			//// //System.out.println("gridId::::" + gridId);
			List gridDataFieldsList = new ArrayList();
			List gridColumnsList = new ArrayList();
			List columnsList = new ArrayList();
			String initialValues = "";
			boolean gridEditable = false;
			String tableName = "";
			JSONObject gridProperties = new JSONObject();
			JSONObject gridOperation = new JSONObject();
			JSONObject hrefObject = new JSONObject();
			JSONObject dropDowndData = new JSONObject();
			String linkedColumns = "";
			String stripValue = "";
			String gridName = "";
			String nvgnFlag = "N";
			String dataSheetFlag = "N", popupEdit = "N";
			String formId = "";
			String panelId = "", uuu_GridRowHeight = "";
			String nestedGridId = "";
			String nestedGridRelId = "";
			String popupEditable = "N";
			JSONObject hiddenColumnObj = new JSONObject();
			JSONObject onChangeFunctionsObj = new JSONObject();
			JSONObject colInitParamsObj = new JSONObject();

			if (true) {

				if (gridList != null && !gridList.isEmpty()) {
					dropDownListDataObj = new JSONObject();

					for (int i = 0; i < gridList.size(); i++) {
						JSONObject childDepenObj = new JSONObject();
						Object[] gridObjectArray = (Object[]) gridList.get(i);
						gridObjectArray[1] = cloudUtills.convertIntoMultilingualValue(labelsObj, gridObjectArray[1]);
						gridObjectArray[16] = cloudUtills.convertIntoMultilingualValue(labelsObj, gridObjectArray[16]);
						gridObjectArray[22] = cloudUtills.convertIntoMultilingualValue(labelsObj, gridObjectArray[22]);
						gridObjectArray[76] = cloudUtills.convertIntoMultilingualValue(labelsObj, gridObjectArray[76]);
						if (gridObjectArray[82] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[82]))) {
							nestedGridId = (String) gridObjectArray[82];
						}
						if (gridObjectArray[83] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[83]))) {
							nestedGridRelId = (String) gridObjectArray[83];
						}
						if (gridObjectArray[15] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[15]))
								&& String.valueOf(gridObjectArray[15]).startsWith("HIDDEN")) {
							hiddenColumnObj.put(gridObjectArray[15], gridObjectArray[20]);
						}

						if (gridObjectArray[47] != null && ((String) gridObjectArray[47]).equalsIgnoreCase("AP")) {
							tbDdwEditFlag = true;
						}

						if (gridObjectArray[20] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[20]))
								&& !"null".equalsIgnoreCase(String.valueOf(gridObjectArray[20]))) {
							Object initialValue = null;
							if (String.valueOf(gridObjectArray[20]).startsWith("ss")) {
								initialValue = request.getParameter(String.valueOf(gridObjectArray[20]));
							} else {
								initialValue = gridObjectArray[20];
							}
							initialValues += "" + gridObjectArray[15] + ":" + initialValue;
							if (i != gridList.size() - 1) {
								initialValues += ",";
							}

						}
						if (i == 0) {
							tableName = (String) gridObjectArray[0];
							gridName = (String) gridObjectArray[1];
							dataSheetFlag = (String) gridObjectArray[11];
							nvgnFlag = (String) gridObjectArray[40];
							if (gridObjectArray[36] != null) {
								String popUpInsertString = ((cloudUtills.clobToString((Clob) gridObjectArray[36])))
										.split("&")[0];
								String[] popupInsertArray = popUpInsertString.split("=");
								if (popupInsertArray.length == 2
										&& popupInsertArray[0].equalsIgnoreCase("uuu_HideFormInsert")) {

									if (popupInsertArray[1].equalsIgnoreCase("N")) {

										popupEdit = "Y";
										JSONObject dataFieldObj = new JSONObject();
										JSONObject columnsObj = new JSONObject();
										dataFieldObj.put("name", "edit_record");
										dataFieldObj.put("type", "string");// 15

										columnsObj.put("text", "");// 3
										columnsObj.put("pinned", true);
										columnsObj.put("editable", false);
										columnsObj.put("datafield", "edit_record");
										columnsObj.put("rendered", "editRecordRendered");
										columnsObj.put("width", ("3" + "%"));// 7
										columnsObj.put("showfilterrow", false);// 7
										columnsObj.put("cellclassname", "edit_record");
										columnsObj.put("cellsrenderer", "edit_recordRenderer");
										columnsObj.put("cellsalign", "left");// 15
										columnsObj.put("align", "center");// 15
//                                columnsObj.put("enabletooltips", true);
										columnsObj.put("filterable", false);
										columnsObj.put("sortable", false);
//                                columnsObj.put("hidden", false);

										gridDataFieldsList.add(dataFieldObj);
										gridColumnsList.add(columnsObj);
									}
								}
							}

							if (dataSheetFlag != null && !"".equalsIgnoreCase(dataSheetFlag)
									&& "Y".equalsIgnoreCase(dataSheetFlag)) {
								JSONObject dataFieldObj = new JSONObject();

								JSONObject columnsObj = new JSONObject();
								dataFieldObj.put("name", "show_detail");
								dataFieldObj.put("type", "string");// 15

								columnsObj.put("text", "");// 3

								columnsObj.put("pinned", ((gridObjectArray[26] != null
										&& ((String) gridObjectArray[26]).equalsIgnoreCase("Y"))));

								columnsObj.put("editable", false);
								columnsObj.put("datafield", "show_detail");
								columnsObj.put("rendered", "dataSheetRendered");
								columnsObj.put("width", ("3" + "%"));// 7
								columnsObj.put("showfilterrow", false);// 7
								columnsObj.put("cellclassname", "show_detail");
								columnsObj.put("cellsalign", "left");// 15
								columnsObj.put("align", "center");// 15
//                                columnsObj.put("enabletooltips", true);
								columnsObj.put("filterable", false);
								columnsObj.put("sortable", false);
//                                columnsObj.put("hidden", false);

								gridDataFieldsList.add(dataFieldObj);
								gridColumnsList.add(columnsObj);
							}
							gridProperties.put("width", gridObjectArray[2] != null ? gridObjectArray[2] : "100%");// for
							// grid
							// Width
							gridProperties.put("height", gridObjectArray[3] != null ? gridObjectArray[3] : "100%");

							gridEditable = ((gridObjectArray[35] != null
									&& ((String) gridObjectArray[35]).equalsIgnoreCase("Y")) ? true : false);
							gridProperties.put("editable", gridEditable);//

							String selectionmode = (String) gridObjectArray[10];
							gridObject.put("selectionmode", selectionmode);
							if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "CHBX".equalsIgnoreCase(selectionmode)) {// For Check Box
								gridProperties.put("editmode", "click");// // SINGLE CLICK
								// gridProperties.put("selectionmode", "checkbox");//
								// gridProperties.put("editmode", "dblclick");// gridProperties.put("editmode",
								// "singlecell");//
								gridProperties.put("selectionmode", "checkbox");//
								// gridProperties.put("editmode", "selectedrow");//
								// gridProperties.put("editmode", "singlecell");//

							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "NONE".equalsIgnoreCase(selectionmode)) {// None
								gridProperties.put("selectionmode", "none");//
							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "SNRW".equalsIgnoreCase(selectionmode)) {// For Single Row
								gridProperties.put("selectionmode", "selectedrow");//
//                                gridProperties.put("editmode", "dblclick");//gridProperties.put("editmode", "singlecell");//
								gridProperties.put("editmode", "click");// gridProperties.put("editmode",
								// "singlecell");//
							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "MTRW".equalsIgnoreCase(selectionmode)) {// For Multiple Rows
								gridProperties.put("selectionmode", "multiplerows");//
//                                gridProperties.put("editmode", "dblclick");
								gridProperties.put("editmode", "click");
							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "MTRE".equalsIgnoreCase(selectionmode)) {// For Multiple Rows Extended
								gridProperties.put("selectionmode", "multiplerowsextended");//
//                                gridProperties.put("editmode", "dblclick");
								gridProperties.put("editmode", "click");
							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "SLCL".equalsIgnoreCase(selectionmode)) {// For Single Cell
								gridProperties.put("selectionmode", "singlecell");//
								gridProperties.put("editmode", "click");//
							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "MTCL".equalsIgnoreCase(selectionmode)) {// For Multiple Cells
								gridProperties.put("selectionmode", "multiplecells");//
							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "MTCE".equalsIgnoreCase(selectionmode)) {// For Multiple Cells Extended
								gridProperties.put("selectionmode", "multiplecellsextended");//
							} else if (selectionmode != null && !"".equalsIgnoreCase(selectionmode)
									&& "MTCA".equalsIgnoreCase(selectionmode)) {// For Multiple Cells Advanced
								gridProperties.put("selectionmode", "multiplecellsadvanced");//
							}
							gridProperties.put("keyboardnavigation", true);// 4
							gridProperties.put("autoshowloadelement", false);// autoshowloadelement: false
							gridProperties.put("columnsresize",
									((gridObjectArray[4] != null && ((String) gridObjectArray[4]).equalsIgnoreCase("Y"))
											? true
											: false));// 4
							gridProperties.put("columnsreorder",
									((gridObjectArray[5] != null && ((String) gridObjectArray[5]).equalsIgnoreCase("Y"))
											? true
											: false));// 5
							gridProperties.put("sortable",
									((gridObjectArray[6] != null && ((String) gridObjectArray[6]).equalsIgnoreCase("Y"))
											? true
											: false));// 6
							if (gridObjectArray[7] != null && ((String) gridObjectArray[7]).equalsIgnoreCase("Y")) {
								gridProperties.put("filterable", true);// 7
								gridProperties.put("showfilterrow", true);// 28
							} else {
								gridProperties.put("filterable", false);// 7
								gridProperties.put("showfilterrow", false);// 28

							}

//                        gridProperties.put("showfilterrow", ((gridObjectArray[28] != null && ((String) gridObjectArray[28]).equalsIgnoreCase("Y")) ? true : false));//28
							gridProperties.put("theme", (gridObjectArray[8] != null ? gridObjectArray[8] : ""));// 8
							gridProperties.put("showtoolbar", true);// 30
//                        gridProperties.put("showtoolbar", ((gridObjectArray[30] != null && ((String) gridObjectArray[30]).equalsIgnoreCase("Y")) ? true : false));//30
							gridProperties.put("pageable",
									((gridObjectArray[9] != null && ((String) gridObjectArray[9]).equalsIgnoreCase("Y"))
											? true
											: false));// 9
							gridProperties.put("enablehover", true);
							gridProperties.put("enablemousewheel", true);

							String gridInitParams = (cloudUtills.clobToString((Clob) gridObjectArray[36]));

							gridOperation = getInitParamObject(gridInitParams);
							if (gridOperation != null && !gridOperation.isEmpty()) {
								uuu_GridRowHeight = (String) gridOperation.get("uuu_GridRowHeight");// uuu_GridRowHeight
								// for grid row
								// height
								popupEditable = (String) gridOperation.get("uuu_popUpFormEditable");
								// scrollmode
								gridProperties.put("scrollmode", "logical");//
								gridProperties.put("enablebrowserselection", true);//

								if (gridOperation.get("uuu_GridSortToggleStates") != null
										&& !"".equalsIgnoreCase(
												String.valueOf(gridOperation.get("uuu_GridSortToggleStates")))
										&& !"null".equalsIgnoreCase(
												String.valueOf(gridOperation.get("uuu_GridSortToggleStates")))) {
									gridProperties.put("sorttogglestates",
											gridOperation.get("uuu_GridSortToggleStates"));//
								}
								if ("Y".equalsIgnoreCase(
										String.valueOf(gridOperation.get("uuu_GridColumnMenuButton")))) {
									gridProperties.put("autoshowcolumnsmenubutton", false);// showing filter menu icon
									// default with out hover
								}
							}

							// System.out.println("gridInitParams:::" + uuu_GridRowHeight);
							gridOperation.put("gridEditable", gridEditable);

							if (gridObjectArray[85] != null) {// 85
								JSONArray pageSizeOptArray = cloudUtills
										.convertJSONArry(String.valueOf(gridObjectArray[85]).split(","));
								gridProperties.put("pagesizeoptions", pageSizeOptArray);
								gridProperties.put("pagesize", pageSizeOptArray.get(0));

							}
							if (gridOperation != null && !gridOperation.isEmpty()) {
								List<Object[]> gridRoleIconsList = null;
								String renderToolbar = "";
								try {
									gridRoleIconsList = getGridRoleIcons(ssRole, gridId);
									if (gridRoleIconsList != null && !gridRoleIconsList.isEmpty()) {
										renderToolbar = cloudUtills.generateRenderToolbar(request, gridRoleIconsList,
												gridId, gridEditable, labelsObj);
									}
								} catch (Exception e) {
								}
								if (!(renderToolbar != null && !"".equalsIgnoreCase(renderToolbar)
										&& !"null".equalsIgnoreCase(renderToolbar))) {
									renderToolbar = cloudUtills.generateRenderToolbar(gridOperation, gridId,
											gridEditable, labelsObj);
								}

								if (renderToolbar != null && !"".equalsIgnoreCase(renderToolbar)
										&& !"null".equalsIgnoreCase(renderToolbar)) {
									gridProperties.put("renderToolbar", renderToolbar);
								}
								if (gridOperation != null && !gridOperation.isEmpty()
										&& gridOperation.get("persInd") != null) {
									gridObject.put("persInd", String.valueOf("persInd"));
									persInd = "Y";
								} else {
									gridObject.put("persInd", "N");
									persInd = "N";
								}
							}

						}

						// start
						JSONObject dataFieldObj = new JSONObject();
						JSONObject columnsObj = new JSONObject();
						dataFieldObj.put("name", gridObjectArray[15]);

						if (gridObjectArray[31] != null && !"".equalsIgnoreCase((String) gridObjectArray[31])) {

							if (gridObjectArray[47] != null && ((String) gridObjectArray[47]).equalsIgnoreCase("T")) {
								dataFieldObj.put("type", "string");

								if (gridObjectArray[50] != null
										&& !"".equalsIgnoreCase(String.valueOf(gridObjectArray[50]))) {
									columnsObj.put("initeditor", "function (row, column, editor) {"
											+ " editor.attr('maxlength'," + gridObjectArray[50] + ");" + " }");
								}

							} else if (gridObjectArray[47] != null
									&& ((String) gridObjectArray[47]).equalsIgnoreCase("C")) {

								dataFieldObj.put("type", "boolean");

							} else if (((String) gridObjectArray[31]).equalsIgnoreCase("date")) {
								dataFieldObj.put("type", gridObjectArray[31]);
								dataFieldObj.put("format",
										((gridObjectArray[39] != null
												&& !"".equalsIgnoreCase((String) gridObjectArray[39]))
														? gridObjectArray[39]
														: "dd-MM-yyyy HH:mm:ss"));
							} else if (gridObjectArray[47] != null
									&& !((String) gridObjectArray[47]).equalsIgnoreCase("L")) {
								dataFieldObj.put("type", gridObjectArray[31]);
							}

						} else {

							dataFieldObj.put("type", "string");
						}

						if (gridObjectArray[47] != null && ((String) gridObjectArray[47]).equalsIgnoreCase("L")) {
							dataFieldObj = new JSONObject();
							dataFieldObj.put("name", gridObjectArray[15]);

							// COMBOBOX VALUES
							JSONObject comboBoxValeuesObj = new JSONObject();
							// //// //System.out.println("LIstbox values :::: "+getListBoxValues((String)
							// gridObjectArray[24], (String)
							// request.getSession(false).getAttribute("ssOrgId"), request));
							dropDownListDataObj.put(gridObjectArray[15], getListBoxValues((String) gridObjectArray[24],
									request.getParameter("ssOrgId"), request));
							// dropDownListDataObj.put(gridObjectArray[15], getResultsByQuery( valueString,
							// "Q", request));

							comboBoxValeuesObj.put("source", "dropDownListData['" + gridObjectArray[15] + "']");
							comboBoxValeuesObj.put("value", "id");
							comboBoxValeuesObj.put("name", "ListboxValue");
							dataFieldObj.put("value", (((String) gridObjectArray[15])));
							dataFieldObj.put("name", (((String) gridObjectArray[15])));
							dataFieldObj.put("values", comboBoxValeuesObj);

							JSONObject orgDataFeild = new JSONObject();
							orgDataFeild.put("value", (((String) gridObjectArray[15])));
							orgDataFeild.put("name", (((String) gridObjectArray[15])) + "_DLOV");
							orgDataFeild.put("values", comboBoxValeuesObj);
							orgDataFeild.put("type", "string");

							gridDataFieldsList.add(orgDataFeild);

						}

						dataFieldObj.put("cellsalign", "center");// 15

						// @dileep
						dataFieldObj.put("regex", gridObjectArray[75]);// 75
						dataFieldObj.put("errorMessage", gridObjectArray[76]);// 76
						boolean isMandatory = false;
						if (gridObjectArray[30] != null && ((String) gridObjectArray[30]).equalsIgnoreCase("M")) {
							isMandatory = true;
							if (gridEditable) {
								columnsObj.put("rendered",
										"function(header) {header.append('<c class=\"visionGridColumnMand" + gridId
												+ gridObjectArray[15]
												+ "\" style=\"color: #FF0000; font-size:10px; font-weight: 500;font-size: large; vertical-align: middle;\"> *<c>');}");
							}
						} else {
							isMandatory = false;
						}
						dataFieldObj.put("dataType", gridObjectArray[47]);
						dataFieldObj.put("COL_SPAN", gridObjectArray[81]);
						dataFieldObj.put("LABEL_MAND", gridObjectArray[91]);
						dataFieldObj.put("COL_MAN", gridObjectArray[30]);
						dataFieldObj.put("COL_FORM_VIEW_FLAG", gridObjectArray[78]);
						columnsObj.put("COL_FORM_VIEW_FLAG", gridObjectArray[78]);
						dataFieldObj.put("isMandatory", isMandatory);
						dataFieldObj.put("colEditType", gridObjectArray[24]);
						dataFieldObj.put("dependencyparams", gridObjectArray[73]);
						// dataFieldObj.put("text", gridObjectArray[16]);
						dataFieldObj.put("label", gridObjectArray[16]);

						gridDataFieldsList.add(dataFieldObj);

						String imageCol = "";
						imageCol = (String) gridObjectArray[15];
						if (imageCol.equalsIgnoreCase("IMAG_IND")) {
							//// //System.out.println("image ind::" + imageCol);
							columnsObj.put("cellsrenderer", "imagerenderer");
							columnsObj.put("text", "Show Image");

						}

						columnsObj.put("text", gridObjectArray[16]);// 3
						columnsObj.put("pinned",
								((gridObjectArray[26] != null && ((String) gridObjectArray[26]).equalsIgnoreCase("Y"))
										? true
										: false));
						String colEditType = (String) gridObjectArray[24];

						if (colEditType != null && !"".equalsIgnoreCase(colEditType)) {
							if ("INV".equalsIgnoreCase(colEditType)) {
								columnsObj.put("hidden", true);
							}

							if ("DISP_ONLY".equalsIgnoreCase(colEditType)) {
								columnsObj.put("editable", false);
							}

							/* Set Cellsrenderer if configured in DAL */
							if (gridObjectArray[84] != null
									&& !String.valueOf(gridObjectArray[84]).trim().equalsIgnoreCase("")) {
								JSONObject ddwData = new JSONObject();
								ddwData.put("ddwId", colEditType);
								ddwData.put("dataFeild", (String) gridObjectArray[15]);
								ddwData.put("gridId", ((String) gridObjectArray[13]).trim());
								columnsObj.put("cellsrenderer", String.valueOf(gridObjectArray[84]));
								ddwData.put("dependencyparams",
										(gridObjectArray[73] != null ? (String) gridObjectArray[73] : ""));
								dropDowndData.put(gridObjectArray[15], ddwData);
								columnsObj.put("editable", false);
							} /* Set Cellsrenderer if configured in DAL */ else if (colEditType.startsWith("DDW")) {

								if (gridObjectArray[47] != null && (((String) gridObjectArray[47]).equalsIgnoreCase("P")
										|| ((String) gridObjectArray[47]).equalsIgnoreCase("AP")
										|| ((String) gridObjectArray[47]).equalsIgnoreCase("TP"))) {
									if (gridEditable) {
										JSONObject ddwData = new JSONObject();
										ddwData.put("ddwId", colEditType);
										ddwData.put("dataFeild", (String) gridObjectArray[15]);
										ddwData.put("gridId", ((String) gridObjectArray[13]).trim());
										columnsObj.put("cellsrenderer", "gridDrpdownRenderor");
										ddwData.put("dependencyparams",
												(gridObjectArray[73] != null ? (String) gridObjectArray[73] : ""));
										dropDowndData.put(gridObjectArray[15], ddwData);
									}

									// columnsObj.put("editable", false);
									//// //System.out.println("gridDrpdownRenderor ::: editable false ::: " +
									// gridObjectArray[15]);
								}

								if (gridObjectArray[47] != null && (((String) gridObjectArray[47]).equalsIgnoreCase("A")
										|| ((String) gridObjectArray[47]).equalsIgnoreCase("AP"))) {

									columnsObj.put("editable", true);
								} else {
									columnsObj.put("editable", false);
								}
								// }

							} else if ("DISP_ONLY".equalsIgnoreCase(colEditType)) {
								columnsObj.put("editable", false);
							} else {
								columnsObj.put("editable", true);
							}

						} else {
							columnsObj.put("editable", true);
						}
						if (gridObjectArray[84] != null
								&& !String.valueOf(gridObjectArray[84]).trim().equalsIgnoreCase("")) {
							columnsObj.put("cellsrenderer", String.valueOf(gridObjectArray[84]));

						}
						if (gridObjectArray[47] != null && ((String) gridObjectArray[47]).equalsIgnoreCase("L")) {

							columnsObj.put("createeditor",
									getCellsRenderers("createeditor", (String) gridObjectArray[15], gridId));
							columnsObj.put("initeditor",
									getCellsRenderers("initeditor", (String) gridObjectArray[15], gridId));
							// columnsObj.put("geteditorvalue", getCellsRenderers("geteditorvalue", (String)
							// gridObjectArray[15]));
							columnsObj.put("datafield", gridObjectArray[15] + "_DLOV");
							columnsObj.put("displayfield", gridObjectArray[15]);
							columnsObj.put("columntype", "dropdownlist");
							columnsObj.put("width", (gridObjectArray[17] + "%"));// 7
							// columnsObj.put("columntype", "template");this is for autocomplete
						} else if (gridObjectArray[47] != null
								&& ((String) gridObjectArray[47]).equalsIgnoreCase("C")) {
							// columnsObj.put("cellsrenderer", "function (row, datafield, columntype, value)
							// {if (value == 'Y' || value == 'y' ){ value=true;return value;}else
							// value=false;return value;}");
							// columnsObj.put("editable", true);
							columnsObj.put("datafield", gridObjectArray[15]);

						} else if (gridObjectArray[47] != null
								&& ((String) gridObjectArray[47]).equalsIgnoreCase("TA")) {

							columnsObj.put("createeditor",
									getTextAreaCellsRenderers("createeditor", (String) gridObjectArray[15], gridId));
							columnsObj.put("initeditor",
									getTextAreaCellsRenderers("initeditor", (String) gridObjectArray[15], gridId));
							columnsObj.put("geteditorvalue",
									getTextAreaCellsRenderers("geteditorvalue", (String) gridObjectArray[15], gridId));
							columnsObj.put("datafield", gridObjectArray[15]);
							columnsObj.put("displayfield", gridObjectArray[15]);
							columnsObj.put("columntype", "template");
							columnsObj.put("width", (gridObjectArray[17] + "%"));// 7

						} else {
							columnsObj.put("datafield", gridObjectArray[15]);
							columnsObj.put("width", (gridObjectArray[17] + "%"));// 7

						}

						String filterType = (String) gridObjectArray[25];
						//// //System.out.println(gridObjectArray[15] + ":::filterType::::" +
						//// filterType);
						if (filterType != null && !"".equalsIgnoreCase(filterType)
								&& !"T".equalsIgnoreCase(filterType)) {

							if ("D".equalsIgnoreCase(filterType)) {// for Date
								columnsObj.put("filtertype", "range");
//                                columnsObj.put("cellsformat", "dd-MM-yyyy");

								if (gridObjectArray[39] != null && !"".equalsIgnoreCase((String) gridObjectArray[39])) {

									columnsObj.put("cellsformat", (String) gridObjectArray[39]);
								} else {
									columnsObj.put("cellsformat", "dd-MM-yyyy");
								}
								columnsObj.put("columntype", "datetimeinput");
							} else if ("L".equalsIgnoreCase(filterType)) {
								columnsObj.put("filtertype", "checkedlist");
								JSONObject items = new JSONObject();
								items.put("FLTR_VALUE", (String) gridObjectArray[37]);
								items.put("FLTR_VALUE_TYPE", (String) gridObjectArray[38]);
								items.put("FLTR_COL_NAME", (String) gridObjectArray[15]);
								items.put("VIEW_NAME", (String) gridObjectArray[14]);

								// List listBoxValuesList = getResultsByQuery(query, "Q", request);
								// List listBoxValuesList = getResultsByQuery(items, request);
								columnsObj.put("filteritems", getResultsByQuery(items, request));
							} else if ("C".equalsIgnoreCase(filterType) || (gridObjectArray[47] != null
									&& ((String) gridObjectArray[47]).equalsIgnoreCase("C"))) {
								columnsObj.put("columntype", "checkbox");
								columnsObj.put("filtertype", "boolean");
							} else if ("F".equalsIgnoreCase(filterType)) { // disable only filter on particlur column
								columnsObj.put("showfilterrow", false);// 7
								columnsObj.put("filterable", false);
								columnsObj.put("sortable", true);
							} else if ("S".equalsIgnoreCase(filterType)) { // disable only sort on particlur column
								columnsObj.put("showfilterrow", true);// 7
								columnsObj.put("filterable", true);
								columnsObj.put("sortable", false);
							} else if ("N".equalsIgnoreCase(filterType)) { // disable bot filter and sort on particlur
								// column
								columnsObj.put("showfilterrow", false);// 7
								columnsObj.put("filterable", false);
								columnsObj.put("sortable", false);
							}
						} else {
							// input
//                              columnsObj.put("filtertype", "input");//for all text filters
							columnsObj.put("filtercondition", "contains");// filtercondition: 'starts_with'
						}
						String columnInitParams = (String) gridObjectArray[57];
						if (columnInitParams != null && !"".equalsIgnoreCase(columnInitParams)
								&& !"null".equalsIgnoreCase(columnInitParams)) {
							JSONObject columnInitParamObj = getInitParamObject(columnInitParams);
							if (columnInitParamObj != null && !columnInitParamObj.isEmpty()) {
								colInitParamsObj.put(gridObjectArray[15], columnInitParamObj);
								if ("N".equalsIgnoreCase(String.valueOf(columnInitParamObj.get("uuu_ColEdtType")))) {
									columnsObj.put("editable", false);
								}
							}

						}

						if ((gridObjectArray[40]) != null) {
							if (((String) gridObjectArray[40]).equalsIgnoreCase("Y")) {
								hrefObject.put("hrefColumn", gridObjectArray[15]);
								columnsObj.put("cellclassname", "vendorno_style");
							}
							if (gridObjectArray[74] != null && !"".equalsIgnoreCase((String) gridObjectArray[74])
									|| gridObjectArray[74] != null
											&& !"".equalsIgnoreCase((String) gridObjectArray[74])) {
								formId = (gridObjectArray[74] != null
										&& !"".equalsIgnoreCase((String) gridObjectArray[74]))
												? ((String) gridObjectArray[74])
												: "";
								panelId = (gridObjectArray[79] != null
										&& !"".equalsIgnoreCase((String) gridObjectArray[79]))
												? ((String) gridObjectArray[79])
												: "";
							}

						}

						if ((((String) gridObjectArray[44]) != null
								&& ((String) gridObjectArray[44]).equalsIgnoreCase("Y"))) {
							hrefObject.put("imageColumn", gridObjectArray[15]);
							hrefObject.put("imageTable", gridObjectArray[45]);
							hrefObject.put("imageTableColumn", gridObjectArray[46]);
						}
						// ////
						// //System.out.println(gridObjectArray[2]+":::::gridObjectArray[39]:::"+gridObjectArray[39]);
						if ((((String) gridObjectArray[41]) != null
								&& ((String) gridObjectArray[41]).equalsIgnoreCase("Y"))) {
							linkedColumns += gridObjectArray[15];
							if (i != gridList.size() - 1) {
								linkedColumns += ",";
							}
						}
						if ((((String) gridObjectArray[42]) != null
								&& ((String) gridObjectArray[42]).equalsIgnoreCase("Y"))) {
							stripValue += (gridObjectArray[15]) + ","
									+ ((((String) gridObjectArray[43]) != null ? (String) gridObjectArray[43]
											: "SPACE"));
							if (i != gridList.size() - 1) {
								stripValue += ";";
							}
						}
						columnsObj.put("cellsalign", "left");// 15
						columnsObj.put("align", "center");// 15
						if (popupEditable != null && "Y".equalsIgnoreCase(popupEditable)) {
							columnsObj.put("editable", false);
						}
						// COL_AUDIT_FLAG -- 32 for Enabling Column wise Tool tips
						if (gridObjectArray[32] != null && "N".equalsIgnoreCase(String.valueOf(gridObjectArray[32]))) {
							// enabletooltips
							columnsObj.put("enabletooltips", false);
						}
						// COL_AUDIT_FLAG -- 32
						// columnsObj.put("rendered", "headerTooltipRenderer");
						columnsList.add(gridObjectArray[15]);
						gridColumnsList.add(columnsObj);

						// COL_CHLD_NULFY
						if (gridObjectArray[86] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[86]))) {
							childDepenObj.put("COL_CHLD_NULFY", gridObjectArray[86]);
						}
						// COL_CHLD_MAN
						if (gridObjectArray[87] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[87]))) {
							childDepenObj.put("COL_CHLD_MAN", gridObjectArray[87]);
						}
						// COL_CHLD_OPT
						if (gridObjectArray[88] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[88]))) {
							childDepenObj.put("COL_CHLD_OPT", gridObjectArray[88]);
						}
						// COL_CHLD_DSBLE
						if (gridObjectArray[89] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[89]))) {
							childDepenObj.put("COL_CHLD_DSBLE", gridObjectArray[89]);
						}
						// COL_CHLD_ENBLE
						if (gridObjectArray[90] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[90]))) {
							childDepenObj.put("COL_CHLD_ENBLE", gridObjectArray[90]);
						}
						dependencyDataObj.put(gridObjectArray[15], childDepenObj);
						if (gridObjectArray[52] != null && !"".equalsIgnoreCase(String.valueOf(gridObjectArray[52]))
								&& gridObjectArray[15] != null
								&& !"".equalsIgnoreCase(String.valueOf(gridObjectArray[15]))) {
							String dataField = (String) gridObjectArray[15];
							if (gridObjectArray[47] != null && ((String) gridObjectArray[47]).equalsIgnoreCase("L")) {
								dataField = (String) gridObjectArray[15] + "_DLOV";
							}
							onChangeFunctionsObj.put(dataField, gridObjectArray[52] + "('" + dataField + "','" + gridId
									+ "','GRID-VIEW','rowIndex')");
						}
					} // for loop
					if ((boolean) gridProperties.get("pageable")) {
						gridProperties.put("virtualmode", true);
						gridProperties.put("rendergridrows", "function(obj) {return obj.data;}");

					}

					if (uuu_GridRowHeight != null && !"".equalsIgnoreCase(uuu_GridRowHeight)) {
						gridProperties.put("rowsheight", Integer.parseInt(uuu_GridRowHeight));
					}

					// for localizaion object
					if (!"en_US".equalsIgnoreCase(String.valueOf(request.getParameter("ssLocale")))) {
						JSONObject localizationobj = new JSONObject();
						localizationobj.put("pagergotopagestring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "Go to page"));
						localizationobj.put("pagershowrowsstring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "Show rows"));
						localizationobj.put("sortascendingstring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "Sort Ascending"));
						localizationobj.put("sortdescendingstring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "Sort Descending"));
						localizationobj.put("sortremovestring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "Remove Sort"));
						localizationobj.put("pagerrangestring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "of"));
						localizationobj.put("pagernextbuttonstring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "next"));
						localizationobj.put("pagerpreviousbuttonstring",
								cloudUtills.convertIntoMultilingualValue(labelsObj, "previous"));
						if (gridOperation != null && !gridOperation.isEmpty()
								&& "Y".equalsIgnoreCase(String.valueOf(gridOperation.get("uuu_FilterPopupNoData")))) {// uuu_FilterPopupNoData
							localizationobj.put("emptydatastring", cloudUtills.convertIntoMultilingualValue(labelsObj,
									"This grid contains huge data. Put filter according to your choice to show the data."));
						} else {
							localizationobj.put("emptydatastring",
									cloudUtills.convertIntoMultilingualValue(labelsObj, "No data to display"));
						}

						// localizestrings
						// System.out.println("localizationobj:::::"+localizationobj);
						gridProperties.put("localization", localizationobj);
						// localization
					} else {
						if (gridOperation != null && !gridOperation.isEmpty()
								&& "Y".equalsIgnoreCase(String.valueOf(gridOperation.get("uuu_FilterPopupNoData")))) {// uuu_FilterPopupNoData
							JSONObject localizationobj = new JSONObject();
							localizationobj.put("emptydatastring", cloudUtills.convertIntoMultilingualValue(labelsObj,
									"This grid contains huge data. Put filter according to your choice to show the data."));
							gridProperties.put("localization", localizationobj);
						}
					}

					gridProperties.put("enabletooltips", true);
					hrefObject.put("linkedColumns", linkedColumns);
					hrefObject.put("stripValue", stripValue);
					gridObject.put("datafields", gridDataFieldsList);
					gridObject.put("columns", gridColumnsList);
					gridObject.put("hrefObj", hrefObject);
					gridObject.put("colsArray", columnsList);
					gridObject.put("tableName", tableName);
					gridObject.put("gridPropObj", gridProperties);
					gridObject.put("gridEditable", gridEditable);
					gridObject.put("gridName", gridName);
					gridObject.put("formId", formId);
					gridObject.put("panelId", panelId);
					gridObject.put("dataSheetFlag", dataSheetFlag);
					gridObject.put("nvgnFlag", nvgnFlag);
					// gridObject.put("gridOperation", getGridOperationIcons(gridOperation, gridId,
					// gridEditable, labelsObj));
					gridObject.put("dropDowndData", dropDowndData);
					gridObject.put("dropDownListData", dropDownListDataObj);
					gridObject.put("initialValues", initialValues);
					gridObject.put("tbDdwEditFlag", tbDdwEditFlag);
					gridObject.put("gridInitParamObj", gridOperation);
					gridObject.put("dependencyObj", dependencyDataObj);
					gridObject.put("hiddenObj", hiddenColumnObj);
					gridObject.put("gridId", gridId);
					gridObject.put("onChangeFunctions", onChangeFunctionsObj);
					gridObject.put("columnInitParamsObj", colInitParamsObj);
					String nogridRefresh = "";
					if (String.valueOf(gridOperation.get("gridNoRefresh")) != null
							&& "N".equalsIgnoreCase(String.valueOf(gridOperation.get("gridNoRefresh")))) {
						nogridRefresh = String.valueOf(gridOperation.get("gridNoRefresh"));
					} else {
						nogridRefresh = "Y";
					}
					gridObject.put("gridRefreshVal", nogridRefresh);// ((paramArray[1] != null &&
					// paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N")
					gridObject.put("persInd", persInd);
					gridObject.put("searchButtonObj", searchButtonObj);
					if (nestedGridRelId != null && !"".equalsIgnoreCase(nestedGridRelId)) {
						gridObject.put("nestedGridRelId", nestedGridRelId);
					}
					if (nestedGridId != null && !"".equalsIgnoreCase(nestedGridId)) {
						gridObject.put("nestedGridId", nestedGridId);
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return gridObject;
	}

	@Transactional
	public List getGridList(String gridId, String role, String orgnId) {
		JSONObject gridListObj = new JSONObject();
		List gridList = new ArrayList();
		try {

			String selectQuery = " SELECT" + " DAL_GRID.GRID_REF_TABLE, "// 0
					+ " DAL_GRID.GRID_DESC,"// 1
					+ " DAL_GRID.GRID_WIDTH, "// 2
					+ " DAL_GRID.GRID_HEIGHT, "// 3
					+ " DAL_GRID.COL_RESIZE_FLAG,"// 4
					+ " DAL_GRID.COL_REOREDER_FLAG,"// 5
					+ " DAL_GRID.SORT_FLAG, "// 6
					+ " DAL_GRID.FILTER_FLAG, "// 7
					+ " DAL_GRID.THEME, "// 8
					+ " DAL_GRID.PAGINATION_FLAG,"// 9
					+ " DAL_GRID.SELECTION_TYPE, "// 10
					+ " DAL_GRID.DATA_SHEET_FLAG,"// 11
					+ " COL_LINK.ROLE_ID, "// 12
					+ " COL_LINK.GRID_ID, "// 13
					+ " COL_LINK.GRID_REF_TABLE as TABLE_NAME, "// 14
					+ " COL_LINK.COL_NAME, "// 15
					+ " COL_LINK.COL_LABEL, "// 16
					+ " COL_LINK.COL_WIDTH, "// 17
					+ " COL_LINK.COL_HEIGHT, "// 18
					+ " COL_LINK.COL_CASE_TYPE, "// 19
					+ " COL_LINK.COL_INIT_VAL, "// 20
					+ " COL_LINK.COL_UPD_VAL, "// 21
					+ " COL_LINK.COL_TOOL_TIP, "// 22
					+ " COL_LINK.COL_REGEX_ID, "// 23
					+ " COL_LINK.COL_EDT_TYPE, "// 24
					+ " COL_LINK.COL_FLTR_TYPE, "// 25
					+ " COL_LINK.COL_PINNED, "// 26
					+ " COL_LINK.COL_SEQ, "// 27
					+ " COL_LINK.COL_PRIM_KEY, "// 28
					+ " COL_LINK.COL_PRIM_KEY_SEQ, "// 29
					+ " COL_LINK.COL_MAN, "// 30
					+ " COL_LINK.COL_DATA_TYPE, "// 31
					+ " COL_LINK.COL_AUDIT_FLAG, "// 32
					+ " COL_LINK.COL_TRIM, "// 33
					+ " COL_LINK.COL_MASK,"// 34
					+ " ROLE_LINK.EDIT_FLAG, "// 35
					+ " ROLE_LINK.GRID_INIT_PARAMS,"// 36
					+ " COL_LINK.COL_FLTR_VALUE,"// 37
					+ " COL_LINK.COL_FLTR_VALUE_TYPE,"// 38
					+ " COL_LINK.COL_DATE_FORMAT,"// 39
					+ " COL_LINK.COL_NAVGN_FLAG,"// 40
					+ " COL_LINK.COL_PARAM_FLAG,"// 41
					+ " COL_LINK.COL_PARAM_STRIP,"// 42
					+ " COL_LINK.COL_STRIP_VALUE,"// 43
					+ " COL_LINK.COL_IMG_PARAM_FLAG,"// 44
					+ " COL_LINK.COL_IMG_TABLE_NAME,"// 45
					+ " COL_LINK.COL_IMG_COL_NAME,"// 46
					+ " COL_LINK.FIELD_TYPE,"// 47
					+ " COL_LINK.ONKEYPRESS_FUNC_NAME,"// 48
					+ " COL_LINK.ONBLUR_FUNC_NAME,"// 49
					+ " COL_LINK.TEXT_MAXLENGTH,"// 50
					+ " COL_LINK.PLACE_HOLDER,"// 51
					+ " COL_LINK.ONCHANGE_FUNC_NAME,"// 52
					+ " COL_LINK.ONCHANGE_PARAM_CNT,"// 53
					+ " COL_LINK.ONCHANGE_PARAMS,"// 54
					+ " COL_LINK.PROGRESS_CNT_FUNC,"// 55
					+ " COL_LINK.SPLIT_CNT,"// 56
					+ " COL_LINK.PARAM_TABLE_NAME,"// 57
					+ " COL_LINK.PARAM_COL_NAME,"// 58
					+ " COL_LINK.FETCH_VIEW_NAME,"// 59
					+ " COL_LINK.ATTR_VALUE,"// 60
					+ " COL_LINK.COND_FLAG,"// 61
					+ " COL_LINK.FILTER_TABLE_NAME,"// 62
					+ " COL_LINK.DELETE_COND_FLAG,"// 63
					+ " COL_LINK.DELETE_COL_NAME,"// 64
					+ " COL_LINK.DELETE_TABLE_NAME,"// 65
					+ " COL_LINK.FETCH_COL_NAME,"// 66
					+ " COL_LINK.SPLIT_COL_DISABLE,"// 67
					+ " COL_LINK.MTXT_DEPENDENCY,"// 68
					+ " COL_LINK.MTXT_REQ,"// 69
					+ " COL_LINK.MTXT_APPL,"// 70
					+ " COL_LINK.STRIP_COL_NAME,"// 71
					+ " COL_LINK.REF_STRIP_COL_NAME,"// 72
					+ " COL_LINK.FIELD_DEPENDENCY_PARAM,"// 73
					+ " COL_LINK.COL_NAVGN_FORM_ID,"// 74
					+ " REGEX.EXPRESSION,"// 75
					+ " REGEX.INFO_MESSAGE,"// 76
					+ " COL_LINK.OPERATOR_ID,"// 77
					+ " COL_LINK.COL_FORM_VIEW_FLAG,"// 78
					+ " COL_LINK.COL_NAVGN_PANEL_ID,"// 79
					+ " COL_LINK.COL_FORM_SEQ,"// 80
					+ " COL_LINK.COL_FORM_COLSPAN,"// 81
					+ " ROLE_LINK.NESTED_GRID_ID, "// 82
					+ " ROLE_LINK.NESTED_GRID_REL_ID,"// 83
					+ " COL_LINK.COL_RENDERER,"// 84
					+ " DAL_GRID.PAGINATION_SIZE,"// 85
					+ " COL_LINK.COL_CHLD_NULFY,"// 86
					+ " COL_LINK.COL_CHLD_MAN,"// 87
					+ " COL_LINK.COL_CHLD_OPT,"// 88
					+ " COL_LINK.COL_CHLD_DSBLE,"// 89
					+ " COL_LINK.COL_CHLD_ENBLE,"// 90
					+ " COL_LINK.COL_LABL_MAN"// 91
					+ " FROM DAL_GRID DAL_GRID,DAL_GRID_ROLE_LINK ROLE_LINK ,DAL_GRID_ROLE_COL_LINK COL_LINK "
					+ " LEFT OUTER JOIN DAL_REGX REGEX ON COL_LINK.COL_REGEX_ID=REGEX.ID"
					+ " WHERE DAL_GRID.GRID_ID = COL_LINK.GRID_ID AND DAL_GRID.GRID_ID = ROLE_LINK.GRID_ID "
					+ " AND COL_LINK.ROLE_ID = ROLE_LINK.ROLE_ID AND COL_LINK.ROLE_ID = :ROLE_ID AND DAL_GRID.GRID_ID =:GRID_ID "
					+ " AND DAL_GRID.ORGN_ID = :ORGN_ID  ORDER BY COL_LINK.COL_SEQ ";

//             System.out.println("selectQuery::single grid ::" + selectQuery);
			Map<String, Object> gridMap = new HashMap();

			gridMap.put("ROLE_ID", role);
			gridMap.put("ORGN_ID", orgnId);
			gridMap.put("GRID_ID", gridId);

//              System.out.println("gridMap:::single grid ::" + gridMap);
			gridList = access.sqlqueryWithParams(selectQuery, gridMap);
//                 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gridList;
	}

	public JSONObject getInitParamObject(String gridInitParams) {
		JSONObject gridOperation = new JSONObject();
		try {
			if (gridInitParams != null && !"".equalsIgnoreCase(gridInitParams)
					&& !"null".equalsIgnoreCase(gridInitParams)) {
				String[] operationIconsArray = gridInitParams.split("&");
				for (int j = 0; j < operationIconsArray.length; j++) {
					String[] paramArray = operationIconsArray[j].split("=");
					if (paramArray != null && paramArray.length != 0) {
						if ("uuu_TableView".equalsIgnoreCase(paramArray[0])) {
							gridOperation.put("tableview",
									((paramArray[1] != null && paramArray[1].equalsIgnoreCase("Y")) ? "Y" : "N"));
						} else if (paramArray[0] != null && !"".equalsIgnoreCase(paramArray[0]) && paramArray[1] != null
								&& !"".equalsIgnoreCase(paramArray[1])) {
							if ("persInd".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("persInd",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_HideGraph".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("umgraph",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_HideEditExport".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("editExportFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_HideUMUpdate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("umupdate",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_RunAnalysis".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("runAnalysis",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_IsUM".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("IsUM",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_HideFormInsert".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("gridformInsert",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_Hide_UnlockUser".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("unlockUsrFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_Hide_ResetUser".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("resetUsrFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_HideInsert".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("addFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_HideUpdate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("editFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("editIcon", "images/update_icon.png");
							} else if ("uuu_SrsNewRegister".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("srsRegisterFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("addIcon", "images/add_icon.png");
							} else if ("uuu_HideDelete".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("deleteFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("deleteIcon", "images/delete_icon.png");
							} else if ("uuu_HideRefresh".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("refreshFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_HidePaging".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("pagingFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("refreshIcon", "images/refresh_icon.png");
							} else if ("uuu_HideImport".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("importFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
//                                    gridOperation.put("importIcon", gridObjectArray[33]);
							} else if ("uuu_importSPIRButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("importSPIRButton",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_verifySPIRButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("verifySPIRButton",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_registerSPIRButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("registerSPIRButton",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_deleteSPIRButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("deleteSPIRButton",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_HideExport".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("exportFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
//                                    gridOperation.put("importIcon", gridObjectArray[33]);
							} else if ("uuu_HideExportExcel".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("exportExcelFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("exportIcon", "");
							} else if ("uuu_HideExportCSV".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("exportCSVFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
								// gridOperation.put("exportIcon", "");
							} else if ("uuu_HideExportPDF".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("exportPDFFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_EncEditable".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("encEditable",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("Y")) ? "Y" : "N"));
//                                    gridOperation.put("exportIcon", gridObjectArray[33]);
							} else if ("uuu_FormView".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("formEditable",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("Y")) ? "Y" : "N"));
//                                    gridOperation.put("exportIcon", gridObjectArray[33]);
							} else if ("uuu_OrderBy".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("orderBy", paramArray[1]);
//                                    gridOperation.put("exportIcon", gridObjectArray[33]);
							} else if ("uuu_GroupBy".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("groupBy", paramArray[1]);
							} else if ("uuu_OpFunName".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("opFunctionName", paramArray[1]);
							} else if ("uuu_GridRowHeight".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("uuu_GridRowHeight", paramArray[1]);
							} else if ("uuu_nonInstance".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("NON-INSTANCE", paramArray[1]);
							} else if ("uuu_instance".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("INSTANCE", paramArray[1]);
							} else if ("uuu_poExt".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("PO_EXT", paramArray[1]);
							} else if ("uuu_ccExt".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("CC_EXT", paramArray[1]);
							} else if ("uuu_poAndCcExt".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("PO_AND_CC_EXT", paramArray[1]);
							} else if ("uuu_soExt".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("SO_EXT", paramArray[1]);
							} else if ("uuu_soAndCcExt".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("SO_AND_CC_EXT", paramArray[1]);
							} else if ("uuu_panelId".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("PANEL_ID", paramArray[1]);
							} else if ("uuu_gridId".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("GRID_ID", paramArray[1]);
							} else if ("uuu_ShowExtPlantGrid".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("ShowExtPlantGrid",
										((paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1])) ? paramArray[1]
												: "N"));
							} else if ("uuu_HideAttachForm".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("gridformAttach",
										((paramArray[1] != null && "N".equalsIgnoreCase(paramArray[1])) ? "Y" : "N"));
							} else if ("uuu_AutoGenerateColumns".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("autoGenerateColumns",
										((paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1])) ? paramArray[1]
												: ""));
							} else if ("uuu_importDomain".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("importDomain",
										((paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1])) ? paramArray[1]
												: ""));
							} else if ("uuu_HideAuditViewFlag".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("auditViewFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_HideAuditGridId".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("auditGridId", paramArray[1]);
							} else if ("uuu_HideclauseColumns".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("clauseColumns", paramArray[1]);
							} else if ("uuu_HideSpirAttachForm".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("uuu_HideSpirAttachForm", paramArray[1]);
							} else if ("uuu_fillDownButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("fillDownButton",
										((paramArray[1] != null && "Y".equalsIgnoreCase(paramArray[1])) ? "Y" : "N"));
							} else if ("uuu_fillDownColumns".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("fillDownColumns", paramArray[1]);
							} else if ("uuu_HideEditableFlag".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("uuu_HideEditableFlag", paramArray[1]);
							} else if ("uuu_HideTableName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("uuu_HideTableName", paramArray[1]);
							} else if ("uuu_HideVariableName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("uuu_HideVariableName", paramArray[1]);
							} else if ("uuu_HideAttachEditableFlag".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("uuu_HideAttachEditableFlag", paramArray[1]);
							} else if ("uuu_massParams".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massParams", paramArray[1]);
							} else if ("uuu_massValidationId".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massValidationId", paramArray[1]);
							} else if ("uuu_massValidate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massValidateButton", paramArray[1]);
							} else if ("uuu_massDHProcess".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massDHProcess", paramArray[1]);
							} else if ("uuu_massProcessData".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("bulkCreate", paramArray[1]);
							} else if ("uuu_massDHProcName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("dhProcName", paramArray[1]);
							} else if ("uuu_massViewName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massViewName", paramArray[1]);
							} else if ("uuu_massPPRSearchButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massPPRSearch", paramArray[1]);
							} else if ("uuu_massCallCopyQuery".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("copyQueryFlag", paramArray[1]);
							} else if ("uuu_runQCTool".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("runQCToolFlag", paramArray[1]);
							} else if ("uuu_massDuplCheckFlag".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("duplCheckFlag", paramArray[1]);
							} else if ("uuu_ClearStagingTable".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("clearStagingTable", paramArray[1]);
							} else if ("uuu_massCopyId".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massCopyId", paramArray[1]);
							} else if ("uuu_masterGridInd".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massMasterGridInd", paramArray[1]);
							} else if ("uuu_masterChngInd".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massMasterChngInd", paramArray[1]);
							} else if ("uuu_massColumnToHide".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massColumnHide", paramArray[1]);
							} else if ("uuu_massTableUpdate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massTableUpdate", paramArray[1]);
							} else if ("uuu_clusterRefresh".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("clusterRefreshFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_HideOpenDoc".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("gridOpenDocument",
										((paramArray[1] != null && "N".equalsIgnoreCase(paramArray[1])) ? "Y" : "N"));
							} else if ("uuu_HideOpenDocClassName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("className", paramArray[1]);
							} else if ("uuu_HideOpenDocMethodName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("methodName", paramArray[1]);
							} else if ("uuu_populateAdminFileBrowser".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("populateAdminFileBrowser",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_gridDownloadTemplate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("downloadTemplate",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_gridCalculateStock".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("calculateStock",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_importColumnToExclude".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("uuu_excludeColumns", paramArray[1]);
							} else if ("uuu_processMrpPlanData".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("processMrpPlanData",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_gridNoRefresh".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("gridNoRefresh",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_gridDataDHURL".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("DHUrl", paramArray[1]);
							} else if ("uuu_genericRegisterButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("registerButtonFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_genericRegisterGridId".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("registerButtonId", paramArray[1]);
							} else if ("uuu_callBapiFlag".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("callBapiFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_calculateBapiName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("calculateBapiName", paramArray[1]);
							} else if ("uuu_tableToUpdate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("calculateTableUpd", paramArray[1]);
							} else if ("uuu_calculateBapiMethodName".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("calculateBapiMethodName", paramArray[1]);
							} else if ("uuu_calculateColumnsToUpdate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("calculateColumnsToUpdate", paramArray[1]);
							} else if ("uuu_calculateWhereCondColumns".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("calculateWhereColumns", paramArray[1]);
							} else if ("uuu_sapFileProcessButton".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("bulkUploadFileProcessFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_HideSapImagesImport".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("bulkUploadImagesImportFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
//                                    gridOperation.put("importIcon", gridObjectArray[33]);
							} else if ("uuu_HideSapDataImport".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("bulkUploadDataImportFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
//                                    gridOperation.put("importIcon", gridObjectArray[33]);
							} else if ("uuu_massexcluderow".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("massexcluderow", paramArray[1]);
							} else if ("uuu_ValidColumns".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("uuu_ValidColumns", paramArray[1]);
							} else if ("uuu_TaxonomyNewTemplate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyNewFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TaxonomyDrTemplate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyDrFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TaxonomyUpdateTemplate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyUpdateFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TaxonomyPropTemplate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyPropertyFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TaxonomyModifyTemplate".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyModifierFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TaxonomyHome".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyHomeFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TaxonomyCloud".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyCloudFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_taxonomyClassDel".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("taxonomyClsDelFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TxmnyAppProcess".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("txmnyAppFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TxmnyDridProcess".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("txmnyDridFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TxmnyDridAppProcess".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("txmnyDridAppFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_TxmnyDridStagingProcess".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("txmnyDridStagingFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_DataHarmImport".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("dataHarmFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_Downloadtemplet".equalsIgnoreCase(paramArray[0])) {
								gridOperation.put("downloadTempletFlag",
										((paramArray[1] != null && paramArray[1].equalsIgnoreCase("N")) ? "Y" : "N"));
							} else if ("uuu_imageUploadLimit".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("uuu_imageUploadLimit", paramArray[1]);
							} else if ("uuu_filesUploadSizeInMB".equalsIgnoreCase(paramArray[0])
									&& (paramArray[1] != null && !"".equalsIgnoreCase(paramArray[1]))) {
								gridOperation.put("uuu_filesUploadSizeInMB", paramArray[1]);
							} else {
								gridOperation.put(paramArray[0], paramArray[1]);
							}
						}
					}

				} // for
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gridOperation;
	}

	@Transactional
	public List getResultsByQuery(JSONObject items, HttpServletRequest request) {
		List resultList = new ArrayList();
		Map<String, Object> parameters = new HashMap<>();
		String valueString = (String) items.get("FLTR_VALUE");
		String valueFlag = (String) items.get("FLTR_VALUE_TYPE");
		String columnsName = (String) items.get("FLTR_COL_NAME");
		String tableName = (String) items.get("VIEW_NAME");

		try {

			if (valueFlag != null && !"".equalsIgnoreCase(valueFlag) && "F".equalsIgnoreCase(valueFlag)) {
				if (valueString != null && !"".equalsIgnoreCase(valueString)) {
					resultList = Arrays.asList(valueString.split(","));
				}

			} else if (valueFlag != null && !"".equalsIgnoreCase(valueFlag) && "Q".equalsIgnoreCase(valueFlag)) {
				if (valueString != null && !"".equalsIgnoreCase(valueString) && valueString.contains("<<--")
						&& valueString.contains("-->>")) {

					String sessionAtt = valueString.substring((valueString.indexOf("<<--")) + 4,
							valueString.indexOf("-->>"));

					String sessionval = (String) request.getSession(false).getAttribute(sessionAtt);

					String replaceval = "<<--" + sessionAtt + "-->>";

					if (sessionval != null && !"".equalsIgnoreCase(sessionval)) {

						String valueString1 = valueString.substring(0, (valueString.indexOf("<<--")));

						String valueString2 = valueString.substring((valueString.indexOf("-->>")) + 4);

						valueString = valueString1 + sessionval + valueString2;
					}
				} else if (valueString == null || "".equalsIgnoreCase(valueString)) {

					valueString = " SELECT DISTINCT " + columnsName + " FROM " + tableName;

				}
				//// //System.out.println("valueString:::" + valueString);
				if (valueString != null && !"".equalsIgnoreCase(valueString)) {
//                    Map<String, Object> parameters = new HashMap<>();
					resultList = access.sqlqueryWithParams(valueString, parameters);

				}
			}
			//// //System.out.println("resultList:::" + resultList);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultList;
	}

	public String getTextAreaCellsRenderers(String renderType, String columnName, String gridId) {
		String result = "";

		String createeditor = "function(row, cellvalue, editor, cellText,width,height) {\n"
				+ "var textareaElement = $('<textarea></textarea>').prependTo(editor);\n" + "textareaElement.css({\n"
				+ "width: width,\n" + "height: height\n" + "});\n" + "}";
		String geteditorvalue = "function(row, cellvalue, editor) {\n" + "return editor.find('textarea').val();\n"
				+ "}";
		String initeditor = "function(row, cellvalue, editor, celltext, pressedkey) {\n"
				+ "var textarea = editor.find('textarea');\n" + "textarea.val(cellvalue);\n" + "}";

		if (renderType != null && !"".equalsIgnoreCase(renderType)) {
			if (renderType.equalsIgnoreCase("createeditor")) {
				result = createeditor;
			} else if (renderType.equalsIgnoreCase("initeditor")) {
				result = initeditor;
			} else if (renderType.equalsIgnoreCase("geteditorvalue")) {
				result = geteditorvalue;
			}
		}

		return result;
	}

	@Transactional
	public JSONArray getListBoxValues(String columnName, String ssOrgId, HttpServletRequest request) {

		JSONArray result = new JSONArray();
		JSONArray cobmoBoxValuesSelectArr = new JSONArray();
		JSONArray finalCobmoBoxValuesSelectArr = new JSONArray();
		//// //System.out.println("inside get COMBOBOXVALUES " + result);
		try {
			JSONObject labelsObject = new JSONObject();

			// columnName="DLOV"+columnName;
			String cobmoBoxValuesSelectQyery = "SELECT DISPLAY,PROCESS_VALUE,DEFAULT_FLAG,DATA_TYPE FROM DAL_DLOV "
					+ " WHERE DLOV_NAME = '" + columnName + "'" + " AND ORGN_ID='" + ssOrgId + "'"
					+ " ORDER BY SEQUENCE_NO ";
			//// //System.out.println("cobmoBoxValuesSelectQyery " +
			//// cobmoBoxValuesSelectQyery);

			Map cobmoBoxValuesSelectMap = new HashMap();
			List cobmoBoxValuesSelectList = access.sqlqueryWithParams(cobmoBoxValuesSelectQyery,
					cobmoBoxValuesSelectMap);
			Object obj = null;

			//// //System.out.println("cobmoBoxValuesSelectList SIZE " +
			//// cobmoBoxValuesSelectList.size());
			if (cobmoBoxValuesSelectList != null && !cobmoBoxValuesSelectList.isEmpty()
					&& cobmoBoxValuesSelectList.size() > 0) {

				//// //System.out.println("cobmoBoxValuesSelectArr2 SIZE after adding both " +
				//// finalCobmoBoxValuesSelectArr.size());
				for (int i = 0; i < cobmoBoxValuesSelectList.size(); i++) {

					obj = cobmoBoxValuesSelectList.get(i);
					JSONObject coboObj = new JSONObject();
					JSONObject coboObj2 = new JSONObject();

					if (obj instanceof Object[]) {

						Object drpDwnRec[] = (Object[]) obj;
						String dataType = (String) drpDwnRec[3];
						String query = (String) drpDwnRec[1];

						if ((dataType != null && query != null) && (!"".equals(dataType) && !"".equals(query))
								&& dataType.equalsIgnoreCase("SQL")) {
							JSONObject items = new JSONObject();
							items.put("FLTR_VALUE", query);
							items.put("FLTR_VALUE_TYPE", "Q");
							items.put("FLTR_COL_NAME", "DAL_DLOV");
							items.put("VIEW_NAME", columnName);
							List listBoxValuesList = getResultsByQuery(items, request);
							if (listBoxValuesList != null && !listBoxValuesList.isEmpty()
									&& listBoxValuesList.size() > 0) {
								for (int j = 0; j < listBoxValuesList.size(); j++) {
									JSONObject jsnObj = new JSONObject();
									obj = listBoxValuesList.get(j);
									if (obj instanceof Object[]) {
										Object queryListBoxObj[] = (Object[]) obj;
										jsnObj.put("id", queryListBoxObj[1]);
										jsnObj.put("ListboxValue", cloudUtills
												.convertIntoMultilingualValue(labelsObject, queryListBoxObj[0]));
//                                        jsnObj.put("ListboxValue", queryListBoxObj[0]);
										jsnObj.put("default", queryListBoxObj[2]);
										finalCobmoBoxValuesSelectArr.add(jsnObj);
									}
								}
							}

						} else {
							coboObj = new JSONObject();
							coboObj.put("id", drpDwnRec[1]);
							coboObj.put("ListboxValue",
									cloudUtills.convertIntoMultilingualValue(labelsObject, drpDwnRec[0]));
							coboObj.put("default", drpDwnRec[2]);

							if (drpDwnRec[1] != null && "Y".equalsIgnoreCase((String) drpDwnRec[2])) {
								finalCobmoBoxValuesSelectArr.add(coboObj);
							} else {
								cobmoBoxValuesSelectArr.add(coboObj);
							}
						}
					}

				}

				for (int j = 0; j < cobmoBoxValuesSelectArr.size(); j++) {
					finalCobmoBoxValuesSelectArr.add((JSONObject) cobmoBoxValuesSelectArr.get(j));
				}
				result = finalCobmoBoxValuesSelectArr;
				//// //System.out.println("result ::: " + result);
			}

		} catch (Exception e) {

			e.printStackTrace();
			//// //System.out.println("Exception ::: " + e.getMessage());
		}

		return result;

	}

	public String getCellsRenderers(String renderType, String columnName, String gridId) {
		String input = "<input/>";
		String columnNameId = "DLOV_" + columnName;
		String result = "";

		String createeditor = "function (row, value, editor) {  " + " var colListBoxData=eval(dropDownListData['"
				+ columnName + "']);" + " var colListboxSource =" + "  {      " + "  datatype: 'array',"
				+ "   datafields: [" + "      { name: 'ListboxValue', type: 'string' },"
				+ "      { name: 'id', type: 'string' }" + "    ]," + "  localdata:colListBoxData" + " };" + " "
				+ "  var colListBoxAdapter = new $.jqx.dataAdapter(colListboxSource);  "
				+ "  editor.jqxDropDownList({ source: colListBoxAdapter,displayMember: 'ListboxValue',"
				+ "  valueMember: 'id',autoDropDownHeight: true});" + "  editor.on('select', function (event) {"
				+ "                    var args = event.args;"
				+ "                    var item =editor.jqxDropDownList('getItem', args.index);"
				+ "                   console.log('on select event fired');"
				+ "                     if (item != null) {" + "                     var lasEditRowInd = $('#" + gridId
				+ "').attr('data-last-ed-row');  return item;"
				// + " $('#" + gridId + "').jqxListBox('selectIndex', item.index);"
				// + " $('#" + gridId + "').jqxGrid('setcellvalue',lasEditRowInd,'" + columnName
				// + "',item.value);"
				// + " $('#" + gridId + "').jqxGrid('endcelledit', lasEditRowInd, '" +
				// columnName + "', false);"
				// + " $('#" + gridId + "').jqxListBox('selectIndex', item.index);"
				// + " $('#" + gridId + "').jqxGrid('setcellvalue',row,'" + columnName +
				// "',item.value);"
				// $("#jqxlistbox").jqxListBox(â€˜selectIndexâ€™, 1);
				+ "                    }});" + "}";
		String geteditorvalue = "function (row, cellvalue, editor) {  " + "$('#" + gridId
				+ "').jqxGrid('setcellvalue',row,'" + columnName + "',editor.val());" + " }";
		String initeditor = "";

		String ddwCellRenderer = "function (row, columnfield, value, defaulthtml, columnproperties) {var cellValue = $(\"#\" + tabId + \"_GRID\").jqxGrid('getcellvalue', row, columnfield);\n"
				+ "                                    var viewType = \"GRID-VIEW\";var ddwData = jsnobj.dropDowndData; var ddwObj = ddwData[columnfield]; var dependencyparams = ddwObj.dependencyparams;\n"
				+ "                                    return \"<div class='propertypopup' style='width:82%;' >\" + cellValue + \"</div><img class='prop_imgClass' src='images/search_icon_color_2.png' style='width:13px;height:13px' onclick=visionDropdown('\" + ddwObj.ddwId.trim() + \"','\" + dependencyparams + \"','\" + viewType + \"','\" + ddwObj.gridId + \"','\" + columnfield + \"','\" + row + \"')>\";\n"
				+ "                                };";

		String autoFillCreateEditor = "function (row, cellvalue,editor, cellText, width, height) { var inputElement = $("
				+ input
				+ ").prependTo(editor); var obj=getAutoCompleteData();var dataArray=autoCmplt.dataArray;inputElement.jqxInput({source: dataArray, displayMember: 'autoFillKey', width: width, height: height});};";
		String autoFillInitEditor = "function (row, cellvalue, editor, celltext, pressedkey) {var inputField = editor.find('input');if (pressedkey) {inputField.val(pressedkey);inputField.jqxInput('selectLast');} else { inputField.val(cellvalue);inputField.jqxInput('selectAll');}};";
		String autoFillGetEditorValue = "function (row, cellvalue, editor) {return editor.find('input').val();}";

		if (renderType != null && !"".equalsIgnoreCase(renderType)) {
			if (renderType.equalsIgnoreCase("createeditor")) {
				result = createeditor;
			} else if (renderType.equalsIgnoreCase("initeditor")) {
				result = initeditor;

				result = " function (row, cellvalue, editor, celltext, pressedkey) {"
						+ "                                    var items = editor.jqxDropDownList('getItems');"
						+ "                                    editor.jqxDropDownList('uncheckAll');"
						+ " editor.jqxDropDownList('open'); "
						// + " if (cellvalue != undefined) {"
						// + " var values = cellvalue.split(/,\\s*/);"
						// + " for (var j = 0; j < values.length; j++) {"
						// + " for (var i = 0; i < items.length; i++) {"
						// + " if (items[i].value == values[j]) {"
						// + " editor.jqxDropDownList('checkIndex', i);"
						// + " }"
						// + " }"
						// + " }"
						// + " }"
						+ "                                }";

			} else if (renderType.equalsIgnoreCase("geteditorvalue")) {
				result = geteditorvalue;

				result = " function (row, cellvalue, editor) {"
						+ "                                    var checkedItems = editor.jqxDropDownList('getCheckedItems');"
						+ "                                    var label = '';"
						+ "                                    for (var i = 0; i < checkedItems.length; i++) {"
						+ "                                        label += checkedItems[i].label;"
						+ "                                        if (i < checkedItems.length - 1) label += ', ' ;"
						+ "                                    }"
						+ "                                    return { label: label  }"
						+ "                                }";
			} else if (renderType.equalsIgnoreCase("ddwCellRenderer")) {
				result = ddwCellRenderer;
			} else if (renderType.equalsIgnoreCase("autoFillCreateEditor")) {
				result = autoFillCreateEditor;
			} else if (renderType.equalsIgnoreCase("autoFillInitEditor")) {
				result = autoFillInitEditor;
			} else if (renderType.equalsIgnoreCase("autoFillGetEditorValue")) {
				result = autoFillGetEditorValue;
			}
		}

		return result;
	}

	@Transactional
	public List<Object[]> getGridRoleIcons(String ssRole, String gridId) {
		List<Object[]> gridRoleIconsList = new ArrayList<>();
		try {
			String selectQuery = "SELECT " + "ROLE_ID, "// 0
					+ "GRID_ID, "// 1
					+ "EDIT_FLAG, "// 2
					+ "PARAM_NAME, "// 3
					+ "SEQUENCE_NO, "// 4
					+ "ICON_PATH, "// 5
					+ "ACTION_URL, "// 6
					+ "FUNCTION_NAME, "// 7
					+ "FUNCTION_PARAMS, "// 8
					+ "OPERATION_NAME, "// 9
					+ "DESCRIPTION, "// 10
					+ "ACTIVE_FLAG, "// 11
					+ "GRID_ICN_CUST_CLMN1, "// 12
					+ "GRID_ICN_CUST_CLMN2, "// 13
					+ "GRID_ICN_CUST_CLMN3, "// 14
					+ "GRID_ICN_CUST_CLMN4, "// 15
					+ "GRID_ICN_CUST_CLMN5, "// 16
					+ "GRID_ICN_CUST_CLMN6, "// 17
					+ "GRID_ICN_CUST_CLMN7, "// 18
					+ "GRID_ICN_CUST_CLMN8, "// 19
					+ "GRID_ICN_CUST_CLMN9, "// 20
					+ "GRID_ICN_CUST_CLMN10 "// 21
					+ " FROM DAL_GRID_ROLE_ICONS WHERE GRID_ID =:GRID_ID AND ROLE_ID =:ROLE_ID AND ACTIVE_FLAG =:ACTIVE_FLAG ORDER BY SEQUENCE_NO";
			Map<String, Object> selectMap = new HashMap<>();
			selectMap.put("GRID_ID", gridId);
			selectMap.put("ACTIVE_FLAG", "Y");
			selectMap.put("ROLE_ID", ssRole);
			gridRoleIconsList = access.sqlqueryWithParams(selectQuery, selectMap);
		} catch (Exception e) {
//          e.printStackTrace();
		}
		return gridRoleIconsList;
	}

	@Transactional
	public String updatechartdata(HttpServletRequest request) {
		String Result = "";
		int updatecount = 0;
		try {
			String tablename = request.getParameter("tableName");
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String filterConditions = request.getParameter("filterConditions");
			String UpdateQuery = "update O_RECORD_VISUALIZATION set FILTER_CONDITION =:FILTER_CONDITION  WHERE CHART_ID =:CHART_ID";
			Map<String, Object> updateMap = new HashMap<>();
			updateMap.put("FILTER_CONDITION", filterConditions); // WHERE_CON
			updateMap.put("CHART_ID", chartId); // chartId
			System.out.println("updateMap:::" + updateMap);
			updatecount = access.executeUpdateSQLNoAudit(UpdateQuery, updateMap);
			if (updatecount != 0) {
				Result = "Updated successFully";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;

	}

	@Transactional
	public JSONObject getconfigobject(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		JSONObject filteredChartConfigObj = new JSONObject();
		try {
			List selectData = null;
			List<String> columnKeys = new ArrayList<>();
			JSONObject chartConfigObj = new JSONObject();
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String chartConfigObjStr = request.getParameter("chartOptAllObj");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			String chartData1 = request.getParameter("chartData");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			System.out.println("LayoutObj :::" + layoutObj);
			System.out.println("DataPropObj :::" + dataPropObj);
			JSONObject framedChartDataObj = getFramedChartDataObject(request, selectData, columnKeys, layoutObj,
					dataPropObj, chartType);
			if (framedChartDataObj != null && !framedChartDataObj.isEmpty()) {
				chartObj.put("data", (JSONObject) framedChartDataObj.get("dataObj"));
				chartObj.put("layout", (JSONObject) framedChartDataObj.get("layoutObj"));
			}
			chartObj.put("dataPropObject", dataPropObj);

//            insertChartDetailsInTable(dataPropObj, dataObj, layoutObj, chartId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	@Transactional
	public JSONObject getCurrentDBTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray treeObjArray = new JSONArray();
		try {
			String fieldName = "";
			String tableName = "";
			String level = request.getParameter("level");
			String filterOperator = request.getParameter("filterOperator");
			String filterValue = request.getParameter("filterValue");
			if (level != null && "0".equalsIgnoreCase(level)) {
				String schemaObjectType = request.getParameter("schemaObjectType");
				String start = request.getParameter("start");
				String limit = request.getParameter("limit");
				if (schemaObjectType != null && "TABLES".equalsIgnoreCase(schemaObjectType)) {
					fieldName = "TABLE_NAME";
					tableName = "USER_TABLES";
				} else if (schemaObjectType != null && "VIEWS".equalsIgnoreCase(schemaObjectType)) {
					fieldName = "VIEW_NAME";
					tableName = "USER_VIEWS";
				}
				String query = "SELECT " + fieldName + " AS FIELD_NAME FROM " + tableName;
				if (filterOperator != null && !"".equalsIgnoreCase(filterOperator) && filterValue != null
						&& !"".equalsIgnoreCase(filterValue)) {
					query += " WHERE  " + fieldName + " " + filterOperator + " '" + filterValue + "' ";
				}
				query = query + " ORDER BY " + fieldName;
				query = "SELECT FIELD_NAME FROM (" + query + ")  OFFSET " + start + " ROWS FETCH NEXT " + limit
						+ " ROWS ONLY";
				System.out.println("query :: " + query);
				List tablesList = access.sqlqueryWithParams(query, Collections.EMPTY_MAP);
				for (int i = 0; i < tablesList.size(); i++) {
					JSONObject treeObj = new JSONObject();
					String table = (String) tablesList.get(i);
					treeObj.put("label", table);
					treeObj.put("description", table);
					JSONArray childArray = new JSONArray();
					JSONObject dummyObj = new JSONObject();
					dummyObj.put("value", "ajax");
					dummyObj.put("label", table);
					childArray.add(dummyObj);
					treeObj.put("items", childArray);
					treeObj.put("value", table);
					treeObjArray.add(treeObj);
				}
			} else if (level != null && "1".equalsIgnoreCase(level)) {

				String gridId = "";
				tableName = request.getParameter("schemaObjectType");

				String gridQuery = "SELECT DISTINCT DAL_GRID.GRID_ID FROM DAL_GRID INNER JOIN DAL_GRID_ROLE_COL_LINK ON DAL_GRID.GRID_ID = DAL_GRID_ROLE_COL_LINK.GRID_ID WHERE DAL_GRID.GRID_REF_TABLE=:GRID_REF_TABLE AND DAL_GRID.ORGN_ID=:ORGN_ID "
						+ "AND DAL_GRID_ROLE_COL_LINK.ROLE_ID=:ROLE_ID";
				Map gridMap = new HashMap();
				gridMap.put("GRID_REF_TABLE", tableName);
				gridMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
				gridMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
				List gridList = access.sqlqueryWithParams(gridQuery, gridMap);
				if (gridList != null && !gridList.isEmpty()) {
					gridId = (String) gridList.get(0);
				}

				if (gridId != null && !"".equalsIgnoreCase(gridId)) {

					Map map = new HashMap();
					String query = "SELECT COL_NAME, COL_LABEL FROM DAL_GRID_ROLE_COL_LINK WHERE" + " GRID_ID=:GRID_ID "
							+ " AND ROLE_ID=:ROLE_ID AND FIELD_TYPE NOT IN('H')";
					System.out.println("query :: " + query);
					map.put("GRID_ID", gridId);
					map.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
					List<Object[]> labelsList = access.sqlqueryWithParams(query, map);
					for (int i = 0; i < labelsList.size(); i++) {
						JSONObject treeObj = new JSONObject();
						String column = (String) labelsList.get(i)[0];
						String columnLabel = (String) labelsList.get(i)[1];
						treeObj.put("label", columnLabel);
						treeObj.put("description", columnLabel);
						JSONArray childArray = new JSONArray();
						JSONObject dummyObj = new JSONObject();
						dummyObj.put("value", "ajax");
						dummyObj.put("label", column);
						childArray.add(dummyObj);
						treeObj.put("items", childArray);
						treeObj.put("value", column);
						treeObjArray.add(treeObj);
					}

				} else {
					String schemaObjectType = request.getParameter("schemaObjectType");
					String query = "SELECT COLUMN_NAME FROM  USER_TAB_COLUMNS  WHERE TABLE_NAME LIKE '"
							+ schemaObjectType + "'";
					System.out.println("query :: " + query);
					List tablesList = access.sqlqueryWithParams(query, Collections.EMPTY_MAP);
					for (int i = 0; i < tablesList.size(); i++) {
						JSONObject treeObj = new JSONObject();
						String table = (String) tablesList.get(i);
						treeObj.put("description", table);
						treeObj.put("label", table);
						JSONArray childArray = new JSONArray();
						JSONObject dummyObj = new JSONObject();
						dummyObj.put("value", "ajax");
						dummyObj.put("label", table);
						childArray.add(dummyObj);
						treeObj.put("items", childArray);
						treeObj.put("value", table);
						treeObjArray.add(treeObj);
					}

				}
			}

			resultObj.put("treeObjArray", treeObjArray);
//         String query = "SELECT "
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getChartFilterData(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String chartId = (String) request.getParameter("chartId");
			String chartType = (String) request.getParameter("chartType");
			String updateflag = (String) request.getParameter("flag");
			String updatechartdata = (String) request.getParameter("filterConditions");
			if (updateflag != null && "Y".equalsIgnoreCase(updateflag)) {
				updatechartdata(request);
			}
			String selectquery = "SELECT " + "X_AXIS_VALUE, "// 0
					+ "Y_AXIS_VALUE,"// 1
					+ "CHART_TYPE,"// 2
					+ "TABLE_NAME,"// 3
					+ "CHART_ID,"// 4
					+ "AGGRIGATE_COLUMNS, "// 5
					+ "FILTER_CONDITION, "// 6
					+ "CHART_PROPERTIES, "// 7
					+ "CHART_CONFIG_OBJECT, "// 8
					+ "VISUALIZE_CUST_COL10, "// 9
					+ "CHART_TITTLE, " // 10
					+ "VISUALIZE_CUST_COL8, " // 11
					+ "VISUALIZE_CUST_COL9, " // 12
					+ "VISUALIZE_CUST_COL5, " // 13
					+ "FILTER_COLUMN, " // 14
					+ "VISUALIZE_CUST_COL6 " // 15
					+ "FROM " + "O_RECORD_VISUALIZATION " + "WHERE " + "CHART_ID =:CHART_ID "
					// + "AND CHART_TYPE =:CHART_TYPE "
					+ "ORDER BY CHART_SEQUENCE_NO";   
			HashMap datamap = new HashMap();
			datamap.put("CHART_ID", chartId);
			// datamap.put("CHART_TYPE", chartType);
			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					JSONObject dataobj = new JSONObject();
					dataobj.put("xAxix", rowData[0]);
					dataobj.put("yAxix", rowData[1]);
					dataobj.put("type", rowData[2]);
					dataobj.put("table", rowData[3]);
					dataobj.put("chartid", rowData[4]);
					dataobj.put("aggColumnName", rowData[5]);
					dataobj.put("filterCondition", rowData[6]);
					dataobj.put("chartPropObj", rowData[7]);
					dataobj.put("chartConfigObj", rowData[8]);
					dataobj.put("labelLegend", rowData[9]);
					dataobj.put("Lebel", rowData[10]);
					dataobj.put("colorsObj", rowData[11]);
					dataobj.put("chartConfigToggleStatus", rowData[12]);
					dataobj.put("compareChartsFlag", rowData[13]);
					dataobj.put("homeFilterColumn", rowData[14]);
					dataobj.put("fetchQuery", rowData[15]);
					dataarr.add(dataobj);
				}
				tabledataobj.put("dataarr", dataarr);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public JSONObject getHomeChartSlicerData(HttpServletRequest request) {
		JSONObject chartSlicerData = new JSONObject();
		Connection connection = null;
		try {
			String result = "";
			String dashBoardName = request.getParameter("chartDropDownVal");
			String resultStr = "<div id='HomeSlicerColumndataId' class = 'HomeSlicerColumndataClass'>"
					+ "<div id=\"VisualizeBIHomeSlicerColumns\"></div>"
					+ "<div id=\"visualizeChartHomeSlicerData\" class=\"visualizeChartHomeSlicerClass\"></div>"
					+ "</div>";
			String tableQuery = "SELECT DISTINCT TABLE_NAME FROM O_RECORD_VISUALIZATION WHERE CHART_TYPE NOT IN('CARD','FILTER','COMPARE_FILTER') AND DASHBORD_NAME =:DASHBORD_NAME";
			Map tableMap = new HashMap();
			tableMap.put("DASHBORD_NAME", dashBoardName);
			List listData = access.sqlqueryWithParams(tableQuery, tableMap);
			if (listData != null && !listData.isEmpty()) {
				for (int j = 0; j < listData.size(); j++) {
					String tableName = (String) listData.get(j);
					Class.forName(dataBaseDriver);
					connection = DriverManager.getConnection(dbURL, userName, password);
					Statement statement = connection.createStatement();
					ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
					ResultSetMetaData metadata = results.getMetaData();
					int columnCount = metadata.getColumnCount();
					if (columnCount > 0) {
						result += "<div  class='visionVisualizeHomeChartTableToggleClass'>";
						result += "<div class='visionVisualizeChartTableClass'><img src=\"images/nextrightarrow.png\" id=\"VisionImageVisualizationTableId\" class=\"VisionImageVisualizationHomeTableClass\" title=\"Show/Hide Table\"/>"
								+ "<img src=\"images/GridDB.png\" class=\"VisionImageVisualizationTableImageClass\" title=\"Show/Hide Table\"/><h6>"
								+ tableName + "</h6></div>";
						result += "<ul class='visionVisualizationDragColumns'>";
						result += "<div class='homechartSlicerColumnsDiv'>";
						for (int i = 1; i <= columnCount; i++) {
							String columnName = metadata.getColumnName(i);
							String columnType = metadata.getColumnTypeName(i);
							String id = tableName + "_" + columnName;
							if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "NUMBER".equalsIgnoreCase(columnType)) {
								result += "<li id=\"" + id
										+ "\" ><img src=\"images/sigma-icon.jpg\" class=\"VisionImageVisualizationHomeTableClass\"/>"
										+ columnName + "</li>";
							} else if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "DATE".equalsIgnoreCase(columnType)) {
								result += "<li id=\"" + id
										+ "\" ><img src=\"images/calendar.svg\" class=\"VisionImageVisualizationHomeTableClass\"/>"
										+ columnName + "</li>";
							} else {
								result += "<li id=\"" + id + "\" >" + columnName + "</li>";
							}
						}
						result += "</div></ul>";
						result += "</div>";
					}
				}

				chartSlicerData.put("result", result);
				chartSlicerData.put("resultStr", resultStr);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartSlicerData;
	}

	@Transactional
	public JSONObject fetchHomeSlicerValues(HttpServletRequest request) {
		JSONObject dataObj = new JSONObject();
		try {
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			int cnt = 0;
			int filterCnt = 0;
			String result = "";
			String selectQuery = "";
			String count = request.getParameter("count");
			String tableName = request.getParameter("id");
			String columnName = request.getParameter("label");
			String divid = request.getParameter("divid");
			String chartType = request.getParameter("chartType");
			String filterCount = request.getParameter("filterCount");
			System.out.println("divid" + divid);

			if (count != null && !"".equalsIgnoreCase(count) && !"null".equalsIgnoreCase(count)) {
				cnt = Integer.parseInt(count);
			}
			if (filterCount != null && !"".equalsIgnoreCase(filterCount) && !"null".equalsIgnoreCase(filterCount)) {
				filterCnt = Integer.parseInt(filterCount);
			}
			String operators = "<select id ='visionVisualizeHomeChartSlicerFieldOperatorsId" + filterCnt
					+ "' class='visionVisualizeHomeChartSlicerOperatorsClass'>" + "<option value= 'IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "In") + "</option>"
					+ "<option value= 'Containing'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Containing") + "</option>"
					+ "<option value= 'EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Equals") + "</option>"
					+ "<option value= 'LIKE'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Like")
					+ "</option>" + "<option value= 'BEGINING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Beginning With") + "</option>"
					+ "<option value= 'ENDING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Ending With") + "</option>"
					+ "<option value= 'NOT EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Equals") + "</option>"
					+ "<option value= 'NOT IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not In") + "</option>"
					+ "<option value= 'IS'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is")
					+ "</option>" + "<option value= 'IS NOT'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is Not") + "</option>"
					+ "<option value= 'NOT LIKE'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Like") + "</option>"
					+ "<option value= 'BETWEEN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Between") + "</option>"
					+ "</select>";
			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)
					&& columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				tableName = tableName.replace("_ID", "");
				selectQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName;
				List selectData = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), 50, 0);
				if (selectData != null && !selectData.isEmpty()) {
					result = "<div id ='visionVisualizeHomeChartSlicerFieldDivId" + filterCnt
							+ "' class='visionVisualizeHomeChartSlicerFieldDivClass'>"
							+ "<div class='visionVisualizeHomeChartSlicerFieldOperator'> <div id ='visionVisualizeHomeChartSlicerFieldId"
							+ filterCnt + "' class='visionVisualizeHomeChartSlicerFieldsClass'>"
							+ "<input type='hidden' id='visionVisualizeHomeChartSlicerHiddenName" + filterCnt
							+ "' value='" + tableName + "." + columnName
							+ "'/><span class='visionVisualizeHomeChartSlicerFieldSpan'>" + columnName
							+ "</span><img src='images/close_white.png' title=\"Remove Column\" onclick=\"RemoveSlicerColumns('"
							+ filterCnt + "','" + chartType + "','" + cnt + "')\"/></div>"
							+ "<div id ='visionVisualizeHomeChartSlicerFieldOperatorsId" + filterCnt
							+ "' class='visionVisualizeHomeChartSlicerFieldOperatorsClass'>" + operators
							+ "</div></div>" + "<div id ='visionVisualizeHomeChartSlicerFieldValuesId" + filterCnt
							+ "' class='visionVisualizeHomeChartSlicerFieldValuesClass' >";
					for (int i = 0; i < selectData.size(); i++) {
						result += "<input type='checkbox' class='visionVisualizeHomeChartSlicerValuesCheckBox' name='visionVisualizeHomeChartSlicerValuesCheckName' value='"
								+ selectData.get(i) + "'>" + selectData.get(i) + "</input>";
						if (i != selectData.size() - 1) {
							result += "<br>";
						}
					}
					result += "</div>" + "</div>";
				}
			}
			dataObj.put("result", result);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dataObj;
	}

	@Transactional
	public JSONObject getSlicerHomeCharts(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			JSONArray tablesArr = new JSONArray();
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String role = (String) request.getSession(false).getAttribute("ssRole");
			String chartDropDownVal = (String) request.getParameter("chartDropDownVal");
			String tablesStr = (String) request.getParameter("tablesArr");
			if (tablesStr != null && !"".equalsIgnoreCase(tablesStr) && !"null".equalsIgnoreCase(tablesStr)) {
				tablesArr = (JSONArray) JSONValue.parse(tablesStr);
			}
			if (tablesArr != null && !tablesArr.isEmpty() && chartDropDownVal != null
					&& !"".equalsIgnoreCase(chartDropDownVal)) {
				String tableName = (String) tablesArr.stream().collect(Collectors.joining(","));
				List tableNamesList = new ArrayList();
				if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)) {
					tableName = new PilogUtilities().trimChar(tableName, ',');
					tableNamesList = Arrays.asList(tableName.split(","));
				}
				String selectquery = "SELECT " + "X_AXIS_VALUE, "// 0
						+ "Y_AXIS_VALUE,"// 1
						+ "CHART_TYPE,"// 2
						+ "TABLE_NAME,"// 3
						+ "CHART_ID,"// 4
						+ "AGGRIGATE_COLUMNS, "// 5
						+ "FILTER_CONDITION, "// 6
						+ "CHART_PROPERTIES, "// 7
						+ "CHART_CONFIG_OBJECT, "// 8
						+ "VISUALIZE_CUST_COL10, "// 9
						+ "VISUALIZE_CUST_COL9, "// 10
						+ "VISUALIZE_CUST_COL8, "// 11
						+ "VISUALIZE_CUST_COL14, "// 12
						+ "VISUALIZE_CUST_COL13, "// 13
						+ "VISUALIZE_CUST_COL18 "// 14
						+ "FROM " + "O_RECORD_VISUALIZATION " + "WHERE DASHBORD_NAME =:DASHBORD_NAME "
						+ "AND ROLE_ID =:ROLE_ID " + "AND TABLE_NAME IN(:TABLE_NAME)"
						+ "AND CHART_TYPE NOT IN ('FILTER','COMPARE_FILTER') " + "ORDER BY CHART_SEQUENCE_NO";
				HashMap datamap = new HashMap();
				datamap.put("DASHBORD_NAME", chartDropDownVal);
				datamap.put("ROLE_ID", role);
				datamap.put("TABLE_NAME", tableNamesList);
				List datalist = access.sqlqueryWithParams(selectquery, datamap);
				if (datalist != null && !datalist.isEmpty()) {
					for (int i = 0; i < datalist.size(); i++) {
						Object[] rowData = (Object[]) datalist.get(i);
						JSONObject dataobj = new JSONObject();
						dataobj.put("xAxix", rowData[0]);
						dataobj.put("yAxix", rowData[1]);
						dataobj.put("type", rowData[2]);
						dataobj.put("table", rowData[3]);
						dataobj.put("chartid", rowData[4]);
						dataobj.put("aggColumnName", rowData[5]);
						dataobj.put("filterCondition", rowData[6]);
						dataobj.put("chartPropObj", rowData[7]);
						dataobj.put("chartConfigObj", rowData[8]);
						dataobj.put("labelLegend", rowData[9]);
						dataobj.put("chartConfigToggleStatus", rowData[10]);
						dataobj.put("colorsObj", rowData[11]);
						dataobj.put("paramCardDateObj", rowData[12]);
						dataobj.put("cardType", rowData[13]);
						dataobj.put("cardTrendType", rowData[14]);
						dataarr.add(dataobj);
					}
					tabledataobj.put("dataarr", dataarr);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public JSONObject movingAvgData(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			String orderBy = "";
			String selectQuery = "";
			String whereCondQuery = "";
			String groupByCond = "";
			JSONObject tableobjdata = new JSONObject();
			String IntervalValues = request.getParameter("IntervalValues");
			String pridictionkey = request.getParameter("pridictionkey");
			String chartId = request.getParameter("chartId");
			List<String> columnKeys = new ArrayList<>();
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject columnobj = new JSONObject();

			tableobjdata = getVisualizationData(request, chartId);
			JSONObject jsobj = new JSONObject();
			String axisColumns = (String) tableobjdata.get("xAxix");
			String valuesColumns = (String) tableobjdata.get("yAxix");
			String chartPropObj = (String) tableobjdata.get("chartPropObj");
			String tableName = (String) tableobjdata.get("table");
			String filterColumns = (String) tableobjdata.get("filterColumns");
            String AxixcolumnName = "";
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String axisColName  = (String)axisColObj.get("columnName");
						if (axisColName != null && !"".equalsIgnoreCase(axisColName)) {
							String[] filteredColumnnameArr = axisColName.split("\\.");
							String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
							columnobj.put("axixColumn", filteredColumnname);
							columnKeys.add(filteredColumnname);
							selectQuery += " " + axisColName + ", ";
							orderBy = axisColName + " ASC";
							groupByCond += " GROUP BY " + axisColName;
						}
					}
				}
			}
			String filteredColumnname = "";
			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject)valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						String valueColName = (String)valueColObj.get("columnName");
						if (valueColName != null && !"".equalsIgnoreCase(valueColName)) {
							String[] filteredColumnnameArr = valueColName.split("\\.");
							filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
							columnKeys.add(filteredColumnname);
                            selectQuery += " " + valueColName + " ";
							if (!(aggColumnName != null && !"".equalsIgnoreCase(aggColumnName) 
									&& !"null".equalsIgnoreCase(aggColumnName))) {
								groupByCond ="";

							}
						}
					}
				}
			}
			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}
			if (orderBy != null && !"".equalsIgnoreCase(orderBy) && !"null".equalsIgnoreCase(orderBy)) {
				orderBy = " ORDER BY " + orderBy;
			}
			selectQuery = "SELECT DISTINCT " + selectQuery + " FROM " + tableName + whereCondQuery + groupByCond
					+ orderBy;
			List selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
			if (selectData != null && selectData.size() > 0) {
				JSONArray jsonDataArr = new JSONArray();
				for (int i = 0; i < selectData.size(); i++) {
					Object[] rowData = (Object[]) selectData.get(i);
					for (int j = 1; j < rowData.length; j++) {
						jsonDataArr.add(rowData[j]);
					}
				}
				if (pridictionkey != null && !"".equalsIgnoreCase(pridictionkey)
						&& "M".equalsIgnoreCase(pridictionkey)) {
					resultobj = movingAverage(request, jsonDataArr, selectData, IntervalValues);
					columnKeys.add("SUMOFAVG");
					resultobj.put("columnKeys", columnKeys);
					resultobj.put("columnObj", columnobj);
				} else if (pridictionkey != null && !"".equalsIgnoreCase(pridictionkey)
						&& "L".equalsIgnoreCase(pridictionkey)) {
//                resultobj = Expoenential(request, jsonDataArr, selectData, IntervalValues);
					resultobj = linearRegression(request, jsonDataArr, selectData);
					columnKeys.add("LINEARREG");
					resultobj.put("columnKeys", columnKeys);
					resultobj.put("columnObj", columnobj);
				} else if (pridictionkey != null && !"".equalsIgnoreCase(pridictionkey)
						&& "E".equalsIgnoreCase(pridictionkey)) {
//                resultobj = Expoenential(request, jsonDataArr, selectData, IntervalValues);
					resultobj = ExponentialMovingAverage(request, jsonDataArr, selectData, IntervalValues);
					columnKeys.add("EMA");
					resultobj.put("columnKeys", columnKeys);
					resultobj.put("columnObj", columnobj);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@Transactional
	public JSONObject getVisualizationData(HttpServletRequest request, String chartId) {
		System.out.println("mdhjf");
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		JSONObject dataobj = new JSONObject();
//        String chartId =  (String)request.getParameter("chartId");      
		try {
//          String RolesData=  getRole(request);
			String selectquery = "SELECT X_AXIS_VALUE, Y_AXIS_VALUE,CHART_TYPE,TABLE_NAME,AGGRIGATE_COLUMNS,FILTER_CONDITION,CHART_PROPERTIES,CHART_CONFIG_OBJECT FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID";
			HashMap datamap = new HashMap();

//            datamap.put("USER_NAME", userName);
			datamap.put("CHART_ID", chartId);
			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					dataobj.put("xAxix", rowData[0]);
					dataobj.put("yAxix", rowData[1]);
					dataobj.put("type", rowData[2]);
					dataobj.put("table", rowData[3]);
					dataobj.put("aggregateColumn", rowData[4]);
					dataobj.put("filterColumns", rowData[5]);
					dataobj.put("chartPropObj", rowData[6]);
					dataobj.put("chartConfigObj", rowData[7]);

				}
//                tabledataobj.put("users", RolesData);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataobj;

	}

	@Transactional
	public JSONObject getFramedMovingAvgDataObject(List selectData, List<String> columnKeys, JSONObject layoutObj,
			JSONObject dataPropObj) {

		JSONArray colorsArr = new JSONArray();
		JSONArray markerColorsArr = new JSONArray();
		JSONObject dataObj = new JSONObject();

		JSONObject framedChartDataObj = new JSONObject();
		if (dataPropObj != null && !dataPropObj.isEmpty()) {
			JSONObject markerObj = (JSONObject) dataPropObj.get("marker");
			if (markerObj != null && !markerObj.isEmpty()) {
				if (markerObj.get("colors") instanceof JSONArray) {
					colorsArr = (JSONArray) markerObj.get("colors");
				} else {
					String colorValues = (String) markerObj.get("colors");
					if (colorValues != null && !"".equalsIgnoreCase(colorValues)
							&& !"null".equalsIgnoreCase(colorValues)) {
						colorsArr.add(colorValues);
					}
				}

			}
		}
		if (selectData != null && !selectData.isEmpty()) {
			int c = 0;
			for (int i = 0; i < selectData.size(); i++) {
				ArrayList datalist = (ArrayList) selectData.get(i);
				for (int j = 0; j < datalist.size(); j++) {
					if (dataObj != null && !dataObj.isEmpty() && dataObj.get(columnKeys.get(j)) != null) {
						JSONArray jsonDataArr = (JSONArray) dataObj.get(columnKeys.get(j));
						jsonDataArr.add(datalist.get(j));
						dataObj.put(columnKeys.get(j), jsonDataArr);
					} else {
						JSONArray jsonDataArr = new JSONArray();
						jsonDataArr.add(datalist.get(j));
						dataObj.put(columnKeys.get(j), jsonDataArr);
					}
				}

				if (colorsArr != null && !colorsArr.isEmpty()) {
					if (c > colorsArr.size() - 1) {
						c = 0;
					}
					markerColorsArr.add(colorsArr.get(c));
				}
				c++;
			}
			framedChartDataObj.put("dataObj", dataObj);
		}

		if (layoutObj != null && !layoutObj.isEmpty()) {
			JSONObject markerObj = (JSONObject) layoutObj.get("marker");
			if (markerObj != null && !markerObj.isEmpty() && markerColorsArr != null && !markerColorsArr.isEmpty()) {
				markerObj.put("colors", markerColorsArr);
			}
			framedChartDataObj.put("layoutObj", layoutObj);
		}

		return framedChartDataObj;
	}

	public JSONObject movingAverage(HttpServletRequest request, JSONArray data, List selectData, String interval) {
		JSONObject resultobjData = new JSONObject();
		List intervatlistData = new ArrayList();
		try {
			int[] intervalarr = Arrays.stream(interval.split(" ")).mapToInt(Integer::parseInt).toArray();
			double[] test = new double[data.size()];
			for (int i = 0; i < data.size(); i++) {
				BigDecimal testValue = (BigDecimal) data.get(i);
				if (testValue != null) {
					test[i] = testValue.doubleValue();
				}
			}
			for (int windSize : intervalarr) {
				int k = 0;
				movingAvg(windSize);
				for (double x : test) {
					newNum(x);
					double SMA = getAvg();
					long v2 = Math.round(SMA); // 129
					System.out.println("Next number = " + x + ", SMA = " + v2);
					Object[] testData = (Object[]) selectData.get(k);

					List<Object> newList = new ArrayList<>(Arrays.asList(testData));
					newList.add(v2);
//                testData[testData.length+1]=SMA;
					k++;
					intervatlistData.add(newList);
				}
				resultobjData.put("chartList", intervatlistData);
				sum = 0;
				period = 0;
				window.clear();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobjData;
	}

	public void newNum(double num) {
		sum += num;
		window.add(num);
		if (window.size() > period) {
			sum -= window.remove();
		}
	}

	public double getAvg() {
		if (window.isEmpty()) {
			return 0.0;
		}
		return sum / window.size();
	}

	public int movingAvg(int period) {
		assert period > 0 : "Period must be a positive integer";
		return this.period = period;
	}

	@Transactional
	public String insertdata(HttpServletRequest request) {
		String result = "";
		int count = 0;
		int recordCount = 0;
		String valuesColumn = "";
		String valuesTable = "";
		int updatecount = 0;
		String chartId = request.getParameter("chartId");
		String chartType = request.getParameter("chartType");
		String username = request.getParameter("username");
		String axisColumns = request.getParameter("axisColumns");
		String valuesColumns = request.getParameter("valuesColumns");
		String aggregateColumns = request.getParameter("aggregateName");
		String columnLebel = request.getParameter("columnLebel");
		String tableName = request.getParameter("tableName");
		String tittle = request.getParameter("tittle");
		String slicer = request.getParameter("slicer");
		String dashbordname = request.getParameter("dashbordname");
		String filterConditions = request.getParameter("filterConditions");
		String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
		String chartPropObj = request.getParameter("chartPropObj");
		JSONObject axisColObj = new JSONObject();
		JSONObject columnsObj = new JSONObject();
		JSONArray axisColsArr = new JSONArray();
		JSONArray valuesColsArr = new JSONArray();

		if (chartType != null && !"".equalsIgnoreCase(chartType) && !"null".equalsIgnoreCase(chartType)
				&& chartType.equalsIgnoreCase("pie") || chartType.equalsIgnoreCase("bar")
				|| chartType.equalsIgnoreCase("donut") || chartType.equalsIgnoreCase("column")
				|| chartType.equalsIgnoreCase("lines") || chartType.equalsIgnoreCase("scatter")) {
			dashbordname = "BASICCHARTS";

		}
		if (chartType != null && !"".equalsIgnoreCase(chartId) && !"null".equalsIgnoreCase(chartId)
				&& chartType.equalsIgnoreCase("waterfall") || chartType.equalsIgnoreCase("funnel")
				|| chartType.equalsIgnoreCase("candlestick") || chartType.equalsIgnoreCase("indicator")) {
			dashbordname = "FINANCIALCHARTS";

		}

		if (chartId != null && !"".equalsIgnoreCase(chartId)) {
			String[] chartid = chartId.split("_");
			Random rd = new Random(); // creating Random object
			chartId = chartid[0] + "_" + rd.nextInt();
		}
		String role = (String) request.getSession(false).getAttribute("ssRole");
		String ssUser = (String) request.getSession(false).getAttribute("ssUsername");
		String roleId = (String) request.getSession(false).getAttribute("ssRole");
		String OrgnId = (String) request.getSession(false).getAttribute("ssOrgId");

		try {

			if (recordCount == 0) {
				String insertQuery = "INSERT INTO O_RECORD_VISUALIZATION(X_AXIS_VALUE, Y_AXIS_VALUE, CHART_TYPE, TABLE_NAME, CREATE_BY,EDIT_BY, CHART_ID,AGGRIGATE_COLUMNS,FILTER_CONDITION,CHART_PROPERTIES,CHART_CONFIG_OBJECT,DASHBORD_NAME, ROLE_ID, ORGN_ID)"
						+ " Values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				Map<Integer, Object> insertMap = new HashMap<>();
				insertMap.put(1, axisColumns); // X_AXIX
				insertMap.put(2, valuesColumns); // Y_AXIX
				insertMap.put(3, chartType); // CHART_TYPE
				insertMap.put(4, tableName); // TABLE_NAME
				insertMap.put(5, username); // USER_NAME
				insertMap.put(6, ssUser); // USER_NAME
				insertMap.put(7, chartId); // CHART_ID
				insertMap.put(8, aggregateColumns); // CHART_ID
				insertMap.put(9, filterConditions); // CHART_ID
				insertMap.put(10, chartPropObj);
				insertMap.put(11, chartConfigPositionKeyStr);
				insertMap.put(12, dashbordname);
				insertMap.put(13, roleId);
				insertMap.put(14, OrgnId);

//                insertMap.put(7, columnLebel);      //CHART_ID
				System.out.println("insertQuery::::::" + insertQuery);
				System.out.println("insertMap::::::" + insertMap);
				count = access.executeNativeUpdateSQLWithSimpleParamsNoAudit(insertQuery, insertMap);
				if (count != 0) {
					result = "published successFully";
				}
			} else {
				result = "Failed to Create";

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;

	}

	@Transactional
	public JSONObject getlandingGraphData(HttpServletRequest request) {
		System.err.println("mdhjf");
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		String chartId = (String) request.getParameter("chartId");

		try {
			String RolesData = getRole(request);
			tabledataobj.put("users", RolesData);
			String selectquery = "SELECT X_AXIS_VALUE, Y_AXIS_VALUE,CHART_TYPE,TABLE_NAME,CHART_ID,AGGRIGATE_COLUMNS,FILTER_CONDITION,CHART_PROPERTIES,CHART_CONFIG_OBJECT FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID";
			HashMap datamap = new HashMap();
			datamap.put("CHART_ID", chartId);

			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					JSONObject dataobj = new JSONObject();
					dataobj.put("xAxix", rowData[0]);
					dataobj.put("yAxix", rowData[1]);
					dataobj.put("type", rowData[2]);
					dataobj.put("table", rowData[3]);
					dataobj.put("chartid", rowData[4]);
					dataobj.put("aggregateType", rowData[5]);
					dataobj.put("filterColumns", rowData[6]);
					dataobj.put("chartPropObj", rowData[7]);
					dataobj.put("chartConfigObj", rowData[8]);
					dataarr.add(dataobj);
				}
				tabledataobj.put("dataarr", dataarr);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;

	}

	@Transactional
	public String getRole(HttpServletRequest request) {
		String result = "";
		try {
			String username = (String) request.getSession(false).getAttribute("ssUsername");
			String selectquery = "SELECT DISTINCT CREATE_BY FROM O_RECORD_VISUALIZATION";
			HashMap datamap = new HashMap();
			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			result = "<div id ='roleContentId' class ='roleContentClass'>";
			result += "<div id ='rolesdataId' class = 'RolesDataClass'>";
			for (int i = 0; i < datalist.size(); i++) {
				result += "<input type='checkbox' class='visionVisualizeChartFiltersValuesCheckBox' name='visionVisualizeChartFiltersValuesCheckName' value='"
						+ datalist.get(i) + "'>" + datalist.get(i) + "</input>";
				if (i != datalist.size() - 1) {
					result += "<br>";
				}
			}
			result += "</div>";
			result += "<div id='applybuttonId' class='applybuttonClass'>"
					+ "<input type='button' value ='Apply' id ='buttonId'></div></div>";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getdashbordname(HttpServletRequest request) {
		JSONArray jsonArray = new JSONArray();
		try {
			String selectQuery = "SELECT DISTINCT DASHBORD_NAME FROM O_RECORD_VISUALIZATION WHERE ORGN_ID = :ORGN_ID AND ROLE_ID = :ROLE_ID";
			HashMap<String, Object> dataMap = new HashMap<>();
			dataMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
			dataMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
			List datalist = access.sqlqueryWithParams(selectQuery, dataMap);
			jsonArray.add("Select");
			jsonArray.add("New");
			for (int i = 0; i < datalist.size(); i++) {
				String dashboardName = (String) datalist.get(i);
				if ((dashboardName != null && !"".equalsIgnoreCase(dashboardName)
						&& !"null".equalsIgnoreCase(dashboardName))) {
					jsonArray.add(dashboardName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("DASHBOARD jsonArray.toString()::" + jsonArray.toString());
		return jsonArray.toString();
	}

	@Transactional
	public JSONObject getJqxPivotGridData1(String gridId, HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		List<Object> pivotGridList = new ArrayList();
		List<JSONObject> dataFieldsList = new ArrayList();
		List<Object> totalFieldList = new ArrayList();
		try {
			JSONObject gridObj = getGrid(gridId, request);
			List labelsList = new ArrayList();
			if (gridObj != null && !gridObj.isEmpty()) {
				dataFieldsList = (List) gridObj.get("datafields");
				String tableName = (String) gridObj.get("tableName");
				if (dataFieldsList != null && !dataFieldsList.isEmpty()) {
					JSONArray resultArr = new JSONArray();
					JSONArray colObjArr = new JSONArray();
					JSONArray filtersObjArr = new JSONArray();
					JSONArray rowObjArr = new JSONArray();
					JSONArray datFieldsObjArr = new JSONArray();
					JSONArray fieldsObjArr = new JSONArray();
					JSONArray valuesObjArr = new JSONArray();
					JSONObject valuesObj = new JSONObject();
					for (int k = 0; k < dataFieldsList.size(); k++) {
						JSONObject dataObj = dataFieldsList.get(k);
						JSONObject datFieldsObj = new JSONObject();
						JSONObject fieldsObj = new JSONObject();
						String colName = (String) dataObj.get("name");
						if (colName != null && !"".equalsIgnoreCase(colName) && !"null".equalsIgnoreCase(colName)
								&& !colName.contains("_DLOV") && !colName.contains("AUDIT_ID")
								&& !colName.contains("HIDDEN")) {
							datFieldsObj.put("name", dataObj.get("name"));
							fieldsObj.put("dataField", dataObj.get("name"));
							datFieldsObj.put("type", dataObj.get("type"));
							fieldsObj.put("text", dataObj.get("label"));
							totalFieldList.add(dataObj.get("name"));
							datFieldsObjArr.add(datFieldsObj);
							fieldsObjArr.add(fieldsObj);
							pivotGridList.add(dataObj.get("label"));
						}
						String type = (String) dataObj.get("type");
						if (type != null && !"".equalsIgnoreCase(type) && !"null".equalsIgnoreCase(type)
								&& "number".equalsIgnoreCase(type)) {
							valuesObj.put("dataField", dataObj.get("name"));
							valuesObj.put("function", "sum");
							valuesObj.put("text", "Sum");
							valuesObj.put("align", "left");
							valuesObjArr.add(valuesObj);
						}
					}
					rowObjArr.add(totalFieldList);
					colObjArr.add(totalFieldList);
					valuesObj.put("function", "sum");
					valuesObj.put("text", "sum");
					valuesObjArr.add(valuesObj);
					String selectQuery = " SELECT ";
					String[] strArray = new String[totalFieldList.size()];
					for (int i = 0; i < totalFieldList.size(); i++) {
						String columnName = (String) totalFieldList.get(i);
						if (columnName != null && !"".equalsIgnoreCase(columnName) && !columnName.contains("_DLOV")
								&& !columnName.contains("AUDIT_ID") && !columnName.contains("HIDDEN")) {
							selectQuery = selectQuery + " " + columnName;
							strArray[i] = columnName.replaceAll("_", " ");
							if (i != totalFieldList.size() - 1) {
								selectQuery = selectQuery + ", ";
							}
						}
					}
					selectQuery = this.cloudUtills.trimChar(selectQuery);
					selectQuery = this.cloudUtills.trimAND(selectQuery);
					selectQuery = selectQuery + " FROM " + tableName;
					List<Object[]> dataList = this.access.sqlqueryWithParams(selectQuery, Collections.EMPTY_MAP);
					if (dataList != null && !dataList.isEmpty()) {
						for (int j = 0; j < dataList.size(); j++) {
							Object[] dataObj = dataList.get(j);
							JSONObject pivotGridObj = new JSONObject();
							for (int m = 0; m < dataObj.length; m++) {
								String listColumn = (String) pivotGridList.get(m);
								if (listColumn != null && "Description".equalsIgnoreCase(listColumn)) {
									String desc = this.cloudUtills.clobToString((Clob) dataObj[m]);
									pivotGridObj.put(totalFieldList.get(m), desc);
								} else {
									pivotGridObj.put(totalFieldList.get(m), dataObj[m]);
								}
							}
							resultArr.add(pivotGridObj);
						}
					}
					resultObj.put("data", resultArr);
					resultObj.put("datafields", datFieldsObjArr);
					resultObj.put("rows", fieldsObjArr);
					resultObj.put("columns", fieldsObjArr);
					resultObj.put("filters", filtersObjArr);
					resultObj.put("values", valuesObjArr);
					resultObj.put("gridId", gridId);
					resultObj.put("columnsList", totalFieldList);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getJqxPivotGridData(String gridId, HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection connection = null;
		List<Object> pivotGridList = new ArrayList();
		List<JSONObject> dataFieldsList = new ArrayList();
		List<Object> totalFieldList = new ArrayList();

		try {
			List labelsList = new ArrayList();
			String tabelname = request.getParameter("tableName");
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tabelname + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			JSONArray resultArr = new JSONArray();
			JSONArray colObjArr = new JSONArray();
			JSONArray filtersObjArr = new JSONArray();
			JSONArray rowObjArr = new JSONArray();
			JSONArray datFieldsObjArr = new JSONArray();
			JSONArray fieldsObjArr = new JSONArray();
			JSONArray valuesObjArr = new JSONArray();
//            JSONObject valuesObj = new JSONObject();
			if (columnCount > 0) {

				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					totalFieldList.add(columnName);
					JSONObject valuesObj = new JSONObject();
					if (columnType != null && !"".equalsIgnoreCase(columnType) && !"null".equalsIgnoreCase(columnType)
							&& "number".equalsIgnoreCase(columnType)) {
						valuesObj.put("dataField", columnName);

						valuesObj.put("function", "sum");
						valuesObj.put("text", "Sum");
						valuesObj.put("align", "left");
						valuesObjArr.add(valuesObj);
					}
				}
			}
//            rowObjArr.add(totalFieldList);
//            colObjArr.add(totalFieldList);
//            valuesObj.put("function", "sum");
//            valuesObj.put("text", "sum");
//            valuesObjArr.add(valuesObj);

			String selectQuery = " SELECT ";
			String[] strArray = new String[totalFieldList.size()];
			for (int i = 0; i < totalFieldList.size(); i++) {
				String columnName = (String) totalFieldList.get(i);
				if (columnName != null && !"".equalsIgnoreCase(columnName) && !columnName.contains("_DLOV")
						&& !columnName.contains("AUDIT_ID") && !columnName.contains("HIDDEN")) {
					selectQuery = selectQuery + " " + columnName;
					strArray[i] = columnName.replaceAll("_", " ");
					if (i != totalFieldList.size() - 1) {
						selectQuery = selectQuery + ", ";
					}
				}
			}
			selectQuery = this.cloudUtills.trimChar(selectQuery);
			selectQuery = this.cloudUtills.trimAND(selectQuery);
			selectQuery = selectQuery + " FROM " + tabelname;
			List<Object[]> dataList = this.access.sqlqueryWithParams(selectQuery, Collections.EMPTY_MAP);
			if (dataList != null && !dataList.isEmpty()) {
				for (int j = 0; j < dataList.size(); j++) {
					Object[] dataObj = dataList.get(j);
					JSONObject pivotGridObj = new JSONObject();
					for (int m = 0; m < dataObj.length; m++) {
						String listColumn = (String) totalFieldList.get(m);

						pivotGridObj.put(totalFieldList.get(m), dataObj[m]);

					}
					resultArr.add(pivotGridObj);
				}
			}
			resultObj.put("data", resultArr);
			resultObj.put("datafields", totalFieldList);
			resultObj.put("rows", fieldsObjArr);
			resultObj.put("columns", fieldsObjArr);
			resultObj.put("filters", filtersObjArr);
			resultObj.put("values", valuesObjArr);
			resultObj.put("gridId", gridId);
			resultObj.put("columnsList", totalFieldList);
			resultObj.put("rows", totalFieldList);
			resultObj.put("columns", totalFieldList);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getPivotGridData(String gridId, HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection connection = null;
		List labelsList = new ArrayList();
		List<Object> pivotGridList = new ArrayList();
		List<JSONObject> dataFieldsList = new ArrayList();
		List<Object> totalFieldList = new ArrayList();
		try {
			String resultString = "";
			String rowsResultString = "";
			String tabelname = request.getParameter("tableName");
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tabelname + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			JSONArray resultArr = new JSONArray();
			JSONArray colObjArr = new JSONArray();
			JSONArray filtersObjArr = new JSONArray();
			JSONArray rowObjArr = new JSONArray();
			JSONArray datFieldsObjArr = new JSONArray();
			JSONArray fieldsObjArr = new JSONArray();
			JSONArray valuesObjArr = new JSONArray();
			JSONObject valuesObj = new JSONObject();
			if (columnCount > 0) {

				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					totalFieldList.add(columnName);
					if (columnType != null && !"".equalsIgnoreCase(columnType) && !"null".equalsIgnoreCase(columnType)
							&& "number".equalsIgnoreCase(columnType)) {
						valuesObj.put("dataField", columnName);
						valuesObj.put("function", "sum");
						valuesObj.put("text", "Sum");
						valuesObj.put("align", "left");
						valuesObjArr.add(valuesObj);
					}
				}
			}
			rowObjArr.add(totalFieldList);
			colObjArr.add(totalFieldList);
			valuesObj.put("function", "sum");
			valuesObj.put("text", "sum");
			valuesObjArr.add(valuesObj);

			String selectQuery = " SELECT ";
			String[] strArray = new String[totalFieldList.size()];
			for (int i = 0; i < totalFieldList.size(); i++) {
				String columnName = (String) totalFieldList.get(i);
				if (columnName != null && !"".equalsIgnoreCase(columnName) && !columnName.contains("_DLOV")
						&& !columnName.contains("AUDIT_ID") && !columnName.contains("HIDDEN")) {
					selectQuery = selectQuery + " " + columnName;
					strArray[i] = columnName.replaceAll("_", " ");
					if (i != totalFieldList.size() - 1) {
						selectQuery = selectQuery + ", ";
					}
				}
			}
			selectQuery = this.cloudUtills.trimChar(selectQuery);
			selectQuery = this.cloudUtills.trimAND(selectQuery);
			selectQuery = selectQuery + " FROM " + tabelname;
			List<Object[]> dataList = this.access.sqlqueryWithParams(selectQuery, Collections.EMPTY_MAP);
			if (dataList != null && !dataList.isEmpty()) {
				for (int j = 0; j < dataList.size(); j++) {
					Object[] dataObj = dataList.get(j);
					JSONObject pivotGridObj = new JSONObject();
					for (int m = 0; m < dataObj.length; m++) {
						String listColumn = (String) totalFieldList.get(m);

						pivotGridObj.put(totalFieldList.get(m), dataObj[m]);

					}
					resultArr.add(pivotGridObj);
				}
			}
			rowsResultString = rowsResultString
					+ "<div class = 'VisionGenericPivotRowsTableSearch' id='VisionPivotRowsResults'>";
			rowsResultString = rowsResultString
					+ "<input type='text' id='pivotTableSearchPvtRows' autocomplete='off' placeholder='Please Enter..'role='textbox' class='visionPivotTableSearch clearable clearable2 ac jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic smartserachclass' data-selected='NO'><a class ='clear_unUsedText'  onclick='clearPivotTextSearch();' ></a> </div>";
			rowsResultString = rowsResultString
					+ "<div class='pivotRowsSearch'> <input type='submit' id='getPivotRowsSearchData' class='pivotRowsSearchIcon' value='' onclick=getPivotSearchResults()  title='Click here to Search/Create'> </div>";
			resultObj.put("data", resultArr);
			resultObj.put("rowsResultString", rowsResultString);
			resultObj.put("datafields", datFieldsObjArr);
			resultObj.put("filters", filtersObjArr);
			resultObj.put("values", valuesObjArr);
			resultObj.put("gridId", gridId);
			resultObj.put("columnsList", totalFieldList);
			resultObj.put("rows", labelsList);
			resultObj.put("columns", labelsList);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public String getGridId(HttpServletRequest request, String TableName) {
		try {
			String Query = "SELECT GRID_ID FROM DAL_GRID WHERE GRID_REF_TABLE =:GRID_REF_TABLE ";
			HashMap datamap = new HashMap();
			List datalist = access.sqlqueryWithParams(Query, datamap);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Transactional
	public String updatechartSettingdata(HttpServletRequest request) {
		String Result = "";
		int updatecount = 0;
		try {
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String chartOptAllObj = request.getParameter("chartOptAllObj");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			String chartConfigToggleStatusStr = request.getParameter("chartConfigToggleStatusStr");
			String UpdateQuery = "update O_RECORD_VISUALIZATION set CHART_PROPERTIES =:CHART_PROPERTIES,CHART_CONFIG_OBJECT = :CHART_CONFIG_OBJECT,"
					+ "VISUALIZE_CUST_COL9=:CHART_CONFIG_TOGGLE_STATUS WHERE CHART_ID =:CHART_ID AND CHART_TYPE =:CHART_TYPE";
			Map<String, Object> updateMap = new HashMap<>();
			updateMap.put("CHART_ID", chartId); // chartId
			updateMap.put("CREATE_BY", (String) request.getSession(false).getAttribute("ssUsername")); // chartId
			updateMap.put("CHART_PROPERTIES", chartOptAllObj);
			updateMap.put("CHART_CONFIG_OBJECT", chartConfigPositionKeyStr);
			updateMap.put("CHART_CONFIG_TOGGLE_STATUS", chartConfigToggleStatusStr);
			updateMap.put("CHART_TYPE", chartType);
			System.out.println("updateMap:::" + updateMap);
			updatecount = access.executeUpdateSQLNoAudit(UpdateQuery, updateMap);
			if (updatecount != 0) {
				Result = "Updated successFully";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;

	}

	@Transactional
	public JSONObject getObjectdata(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONArray dataFieldsArray = new JSONArray();
		JSONArray columnsArray = new JSONArray();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection connection = null;

		int recordsCount = 0;
		try {
			String chartId = request.getParameter("chartId");
			String paramsData = request.getParameter("paramArray");
			String tableName = "";
			if (chartId != null && !"".equalsIgnoreCase(chartId) && !"null".equalsIgnoreCase(chartId)) {
				String tableQuery = "SELECT TABLE_NAME FROM O_RECORD_VISUALIZATION WHERE CHART_ID=:CHART_ID "
						+ "AND ORGN_ID=:ORGN_ID AND ROLE_ID=:ROLE_ID";
				Map tableMap = new HashMap();
				tableMap.put("CHART_ID", chartId);
				tableMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
				tableMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
				List tableList = access.sqlqueryWithParams(tableQuery, tableMap);
				if (tableList != null && !tableList.isEmpty()) {
					tableName = (String) tableList.get(0);
					if (tableName != null && !"".equalsIgnoreCase(tableName)) {
						String gridQuery = "SELECT DISTINCT DAL_GRID.GRID_ID FROM DAL_GRID INNER JOIN DAL_GRID_ROLE_COL_LINK ON DAL_GRID.GRID_ID = DAL_GRID_ROLE_COL_LINK.GRID_ID WHERE DAL_GRID.GRID_REF_TABLE=:GRID_REF_TABLE AND DAL_GRID.ORGN_ID=:ORGN_ID "
								+ "AND DAL_GRID_ROLE_COL_LINK.ROLE_ID=:ROLE_ID";
						Map gridMap = new HashMap();
						gridMap.put("GRID_REF_TABLE", tableName);
						gridMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
						gridMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
						List gridList = access.sqlqueryWithParams(gridQuery, gridMap);
						if (gridList != null && !gridList.isEmpty()) {
							String gridId = (String) gridList.get(0);
							if (gridId != null && !"".equalsIgnoreCase(gridId)) {
								resultObj.put("gridId", gridId);
								resultObj.put("gridObj", getGrid(gridId, request));
								return resultObj;
							}
						}
					}
				}
			}

			String groupscount = request.getParameter("groupscount");
			String pagenum = request.getParameter("pagenum");
			String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize") : "10";
			String recordendindex = request.getParameter("recordendindex");
			String recordstartindex = (request.getParameter("recordstartindex"));

			String getOnlyDataArray = (request.getParameter("getOnlyDataArray"));
			connection = DriverManager.getConnection(dbURL, userName, password);
			int startIndex = 0;
			int endIndex = 0;
			if (recordstartindex != null && recordendindex != null && pagesize != null) {
				startIndex = Integer.parseInt(recordstartindex);
				endIndex = Integer.parseInt(recordendindex);
			}

			String conditionQuery = "";

			Integer filterscount = 0;
			String filterCondition = "";
			String selectQuery = "SELECT * FROM " + tableName;

			String condition = "";
			if (paramsData != null && !"".equalsIgnoreCase(paramsData) && !"null".equalsIgnoreCase(paramsData)) { // nested
				JSONArray paramsDataArr = (JSONArray) JSONValue.parse(paramsData);
				if (paramsDataArr != null && !paramsDataArr.isEmpty()) {
					condition += paramsDataArr.stream().filter(params -> (params != null))
							.map(paramFilterData -> buildCondition((JSONObject) paramFilterData, request,
									dataBaseDriver, "N"))
							.collect(Collectors.joining(" AND "));
				}
				if (condition != null && !"".equalsIgnoreCase(condition) && !"".equalsIgnoreCase(condition)) {
					if (selectQuery.contains("WHERE")) {
						selectQuery += " AND " + condition;
					} else {
						selectQuery += " WHERE " + condition;
					}
				}
			}

			if (request.getParameter("filterscount") != null) {
				filterscount = new Integer(request.getParameter("filterscount"));
				filterCondition = buildFilterCondition(filterscount, request, dataBaseDriver);
				if (filterCondition != null && !"".equalsIgnoreCase(filterCondition)
						&& !"null".equalsIgnoreCase(filterCondition)) {
					if (selectQuery.contains("WHERE")) {
						selectQuery += " AND " + filterCondition;
					} else {
						selectQuery += " WHERE " + filterCondition;
					}
				}

			}

			String countQuery = "SELECT count(*) FROM (" + selectQuery + " )";
			ResultSet countResultSet = connection.prepareStatement(countQuery).executeQuery();
			while (countResultSet.next()) {
				recordsCount = countResultSet.getInt(1);

			}

			String orderby = "";
			String sortdatafield = request.getParameter("sortdatafield");
			System.out.println("sortdatafield::::" + sortdatafield);
			String sortorder = request.getParameter("sortorder");
			if (!(sortdatafield != null && !"".equalsIgnoreCase(sortdatafield))) {
				sortdatafield = (String) request.getAttribute("sortdatafield");
			}
			if (!(sortorder != null && !"".equalsIgnoreCase(sortorder))) {
				sortorder = (String) request.getAttribute("sortorder");
			}
			System.out.println("sortorder::::" + sortorder);
			if (sortdatafield != null && sortorder != null && (sortorder.equals("asc") || sortorder.equals("desc"))) {
				orderby = " ORDER BY " + sortdatafield + " " + sortorder;
			}

			selectQuery += orderby;
			if (dataBaseDriver.toUpperCase().contains("ORACLE")) {
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			} else if (dataBaseDriver.toUpperCase().contains("MYSQL")) {
				conditionQuery += " LIMIT " + startIndex + "," + pagesize + "";
			} else if (dataBaseDriver.toUpperCase().contains("MSSQL")) {
				if (!(orderby != null && !"".equalsIgnoreCase(orderby) && !"null".equalsIgnoreCase(orderby))) {
					selectQuery += " ORDER BY (SELECT NULL) ";
				}
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			}

			selectQuery = selectQuery + conditionQuery;

			System.out.println("Tree Data query::" + selectQuery);
			selectQuery = selectQuery.replaceAll("\\s+", " ");
            selectQuery = selectQuery.replace("AND GROUP BY", "GROUP BY");
            selectQuery = selectQuery.replace("#", " "); // Replace # with space
            selectQuery = selectQuery.replace("$", ",");
            System.out.println("AFTER removing # AND $ ::Tree Data query::" + selectQuery);  
			preparedStatement = connection.prepareStatement(selectQuery);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			int columnCount = resultSetMetaData.getColumnCount();

			if (getOnlyDataArray != null && "Y".equalsIgnoreCase(getOnlyDataArray)) {

				while (resultSet.next()) {
					JSONObject dataObj = new JSONObject();

					for (int i = 1; i <= columnCount; i++) {
						JSONObject dataFieldsObj = new JSONObject();
						String columnType = resultSetMetaData.getColumnTypeName(i);
						String columnName = resultSetMetaData.getColumnName(i);
						Object data = null;
						if ("DATE".equalsIgnoreCase(columnType) || "DATETIME".equalsIgnoreCase(columnType)
								|| "TIMESTAMP".equalsIgnoreCase(columnType)) {
							data = resultSet.getString(columnName);
						} else if (columnType != null && !"".equalsIgnoreCase(columnType)
								&& "CLOB".equalsIgnoreCase(columnType)) {
							String popUpInsertString = new PilogUtilities()
									.clobToString((Clob) resultSet.getClob(columnName));
							if (popUpInsertString != null && !"".equalsIgnoreCase(popUpInsertString)) {
								data = popUpInsertString;
							}
						} else {
							data = resultSet.getObject(columnName);
						}
						if (data instanceof byte[]) {
							byte[] bytesArray = (byte[]) data;
							data = new RAW(bytesArray).stringValue();
						}
						dataObj.put(columnName, data);

					}

					dataArray.add(dataObj);

				}
				if (recordsCount != 0) {
					dataArray.add(recordsCount);
				}

				resultObj.put("dataArray", dataArray);
			} else {
				for (int i = 1; i <= columnCount; i++) {
					JSONObject dataFieldsObj = new JSONObject();
					String columnType = resultSetMetaData.getColumnTypeName(i);
					String columnName = resultSetMetaData.getColumnName(i);
					dataFieldsObj.put("name", columnName);
					dataFieldsObj.put("type", "string");

					dataFieldsArray.add(dataFieldsObj);

					JSONObject columnsObject = new JSONObject();
					String colLabel = (columnName).toLowerCase().replace("_", " ");
					colLabel = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
							.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
							.collect(Collectors.joining(" "));
					columnsObject.put("text", colLabel);
					columnsObject.put("datafield", columnName);
					columnsObject.put("width", 120);
					columnsObject.put("sortable", true);
					columnsArray.add(columnsObject);

				}

				resultObj.put("dataFieldsArray", dataFieldsArray);
				resultObj.put("columnsArray", columnsArray);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String buildFilterCondition(int filterscount, HttpServletRequest request, String dataBaseDriver) {
		String conditionQuery = "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

			for (int i = 0; i < filterscount; i++) {
				String columnName = request.getParameter("filterdatafield" + i);
				String condition = request.getParameter("filtercondition" + i);
				String value = request.getParameter("filtervalue" + i);
				String filteroperator = request.getParameter("filteroperator" + i);
				String condtionQuery = "";
				value = value.toUpperCase();

				if (columnName.contains("DATE")) {
					value = value.substring(0, value.indexOf("GMT") - 9).trim();
					if (dataBaseDriver != null && !"".equalsIgnoreCase(dataBaseDriver)) {
						if (dataBaseDriver.toUpperCase().contains("ORACLE")) {
							columnName = "TO_DATE(TO_CHAR(" + columnName + ",'DY MON DD YYYY'), 'DY MON DD YYYY')";
							value = "TO_DATE('" + value + "','DY MON DD YYYY')";
						} else if (dataBaseDriver.toUpperCase().contains("MYSQL")) {
							columnName = "STR_TO_DATE(DATE_FORMAT(" + columnName
									+ ",'DY MON DD YYYY'), 'DY MON DD YYYY')";
							value = "STR_TO_DATE('" + value + "','DY MON DD YYYY')";
						} else if (dataBaseDriver.toUpperCase().contains("MSSQL")) {
							columnName = "CONVERT(CONVERT(VARCHAR(10)," + columnName
									+ ",'DY MON DD YYYY'), 'DY MON DD YYYY')";
							value = "CONVERT('" + value + "','DY MON DD YYYY')";
						} else if (dataBaseDriver.toUpperCase().contains("DB2")) {

						}

					}

				}

				if (condition != null && !"".equalsIgnoreCase(condition)) {

					String query = "";
					switch (condition) {
						case "CONTAINS":
							query = "UPPER(" + columnName + ") LIKE '%" + splitValue(value) + "%'";
							break;

						case "DOES_NOT_CONTAIN":
							query = " " + columnName + " NOT LIKE '%" + splitValue(value) + "%'";
							break;

						case "EQUAL":
							query = " " + columnName + " = '" + value + "'";
							break;
						case "NOT_EQUAL":
							query = " " + columnName + " != '" + value + "'";
							break;

						case "GREATER_THAN":
							query = " " + columnName + " > '" + value + "'";
							break;
						case "LESS_THAN":
							query = " " + columnName + " < '" + value + "'";
							break;

						case "STARTS_WITH":
							query = " " + columnName + " LIKE '" + value + "%'";

							break;
						case "ENDS_WITH":
							query = " " + columnName + " LIKE '%" + value + "'";
							break;

						case "NULL":
							query = " " + columnName + " IS  NULL";
							break;
						case "NOT_NULL":
							query = " " + columnName + " IS NOT NULL";
							break;
						case "GREATER_THAN_OR_EQUAL":
							query = " " + columnName + " >= " + value + "";
							break;
						case "LESS_THAN_OR_EQUAL":
							query = " " + columnName + " <= " + value + "";
							break;

					}
					if (query != null && !"".equalsIgnoreCase(query)) {
						conditionQuery += query;
						if (i != filterscount - 1) {
							conditionQuery += " AND ";
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return conditionQuery;
	}

	public String buildCondition(JSONObject paramObj, HttpServletRequest request, String dataBaseDriver,
			String dlovColNameFlag) {
		String conditionQuery = "";
		try {
			String operatorName = (String) paramObj.get("operator");
			String value = (String) paramObj.get("value");
			String columnName = (String) paramObj.get("column");
			String type = (String) paramObj.get("datatype");
			if (dlovColNameFlag != null && "Y".equalsIgnoreCase(dlovColNameFlag)) {
				columnName = (String) paramObj.get("dlovcolname");
				value = (String) paramObj.get("typeSelectStr");
			}

			System.out.println(columnName + ":::type::::" + type);
			if (value != null && !"".equalsIgnoreCase(value) && !"null".equalsIgnoreCase(value)) {
				value = value.toUpperCase();
			}

			if (columnName != null && columnName.endsWith("DATE")) {

				if (dataBaseDriver != null && !"".equalsIgnoreCase(dataBaseDriver)) {
					if (dataBaseDriver.toUpperCase().contains("ORACLE")) {
						columnName = "TO_DATE(TO_CHAR(" + columnName + ",'DD-MM-YYYY'), 'DD-MM-YYYY')";
						value = "TO_DATE('" + value + "','DD-MM-YYYY')";
						if ("BETWEEN".equalsIgnoreCase(operatorName)) {
							String minValue = (String) paramObj.get("minvalue");
							String maxvalue = (String) paramObj.get("maxvalue");
							if (!(minValue != null && !"".equalsIgnoreCase(minValue))) {
								minValue = "01-01-1947";
							}
							if (!(maxvalue != null && !"".equalsIgnoreCase(maxvalue))) {
								Date date = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
								maxvalue = formatter.format(date);
							}
							value = "TO_DATE('" + minValue + "','DD-MM-YYYY') AND TO_DATE('" + maxvalue
									+ "','DD-MM-YYYY')";
						}
					} else if (dataBaseDriver.toUpperCase().contains("MYSQL")) {
						columnName = "STR_TO_DATE(DATE_FORMAT(" + columnName + ",'DD-MM-YYYY'), 'DD-MM-YYYY')";
						value = "STR_TO_DATE('" + value + "','DD-MM-YYYY')";
						if ("BETWEEN".equalsIgnoreCase(operatorName)) {
							String minValue = (String) paramObj.get("minvalue");
							String maxvalue = (String) paramObj.get("maxvalue");
							if (!(minValue != null && !"".equalsIgnoreCase(minValue))) {
								minValue = "01-01-1947";
							}
							if (!(maxvalue != null && !"".equalsIgnoreCase(maxvalue))) {
								Date date = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
								maxvalue = formatter.format(date);
							}
							value = "STR_TO_DATE('" + minValue + "','DD-MM-YYYY') AND STR_TO_DATE('" + maxvalue
									+ "','DD-MM-YYYY')";
						}
					} else if (dataBaseDriver.toUpperCase().contains("SQLSERVER")) {
						columnName = "CONVERT(CONVERT(VARCHAR(10)," + columnName + ",'DD-MM-YYYY'), 'DD-MM-YYYY')";
						value = "CONVERT('" + value + "','DD-MM-YYYY')";
						if ("BETWEEN".equalsIgnoreCase(operatorName)) {
							String minValue = (String) paramObj.get("minvalue");
							String maxvalue = (String) paramObj.get("maxvalue");
							if (!(minValue != null && !"".equalsIgnoreCase(minValue))) {
								minValue = "01-01-1947";
							}
							if (!(maxvalue != null && !"".equalsIgnoreCase(maxvalue))) {
								Date date = new Date();
								SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
								maxvalue = formatter.format(date);
							}
							value = "CONVERT('" + minValue + "','DD-MM-YYYY') AND CONVERT('" + maxvalue
									+ "','DD-MM-YYYY')";
						}
					} else if (dataBaseDriver.toUpperCase().contains("DB2")) {

					}

				}

			}

			if (operatorName != null && !"".equalsIgnoreCase(operatorName) && !"null".equalsIgnoreCase(operatorName)) {
				operatorName = operatorName.toUpperCase();
				switch (operatorName) {
					case "CONTAINING":
						conditionQuery = "UPPER(" + columnName + ") LIKE '%" + value + "%'";
						break;
					case "EQUALS":
						if (columnName.contains("_DATE")) {
							conditionQuery = " " + columnName + " = " + value + "";
						} else {
							conditionQuery = "UPPER(" + columnName + ") = '" + value + "'";
						}
						break;
					case "NOT EQUALS":
						if (columnName.contains("_DATE")) {

							conditionQuery = " " + columnName + " != " + value + "";
						} else {
							conditionQuery = "UPPER(" + columnName + ") != '" + value + "'";
						}
						break;

					case "GREATER THAN":
						if (columnName.contains("_DATE")) {

							conditionQuery = " " + columnName + " > " + value + "";

						} else {
							conditionQuery = " " + columnName + " > '" + value + "'";
						}
						break;
					case "LESS THAN":
						if (columnName.contains("_DATE")) {
							conditionQuery = " " + columnName + " < " + value + "";
						} else {
							conditionQuery = " " + columnName + " < '" + value + "'";
						}
						break;

					case "BEGINING WITH":
						conditionQuery = " " + columnName + " LIKE '" + value + "%'";

						break;
					case "ENDING WITH":
						conditionQuery = " " + columnName + " LIKE '%" + value + "'";
						break;
					case "LIKE":
						conditionQuery = "UPPER(" + columnName + ") LIKE '" + value + "'";
						break;
					case "NOT LIKE":
						conditionQuery = "UPPER(" + columnName + ") NOT LIKE '" + value + "'";
						break;
					case "IS":
						conditionQuery = " " + columnName + " IS  NULL";
						break;
					case "IS NOT":
						conditionQuery = " " + columnName + " IS NOT NULL";
						break;
					case ">":
						conditionQuery = " " + columnName + " > '" + value + "'";
						break;
					case "<":
						conditionQuery = " " + columnName + " < '" + value + "'";
						break;
					case ">=":
						conditionQuery = " " + columnName + " >= " + value + "";
						break;
					case "<=":
						conditionQuery = " " + columnName + " <= " + value + "";
						break;
					case "IN":

						conditionQuery = "UPPER(" + columnName + ") IN " + generateInStr(value) + "";
						break;
					case "NOT IN":
						conditionQuery = "UPPER(" + columnName + ") NOT IN " + generateInStr(value) + "";
						break;
					case "BETWEEN":
						conditionQuery = " " + columnName + " BETWEEN " + value;
						break;

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conditionQuery;
	}

	public String splitValue(String value) {

		try {
			System.err.println("value:::Before:::" + value);
			if (value != null && !"".equalsIgnoreCase(value)) {
				value = value.replaceAll(" ", "%");
			}
			System.err.println("value:::After:::" + value);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	public JSONObject ExponentialMovingAverage(HttpServletRequest request, JSONArray data, List selectData,
			String interval) {
		JSONObject resultobjData = new JSONObject();
		List intervatlistData = new ArrayList();
		double[] results;
		double smoothingConstant = 0.0;
		double[] periodSma;
		double[] periodEma;
		periodSma = new double[data.size()];
		periodEma = new double[data.size()];

		try {
			int[] intervalarr = Arrays.stream(interval.split(" ")).mapToInt(Integer::parseInt).toArray();
			double[] test = new double[data.size()];
			for (int i = 0; i < data.size(); i++) {
				BigDecimal testValue = (BigDecimal) data.get(i);
				if (testValue != null) {
					test[i] = testValue.doubleValue();
				}
			}
			for (int windSize : intervalarr) {
				int k = 0;
				smoothingConstant = 2d / (windSize + 1);
				DashBoardsDAO cal = new DashBoardsDAO();
				for (int i = (windSize - 1); i < test.length; i++) {
					double[] slice = Arrays.copyOfRange(test, 0, i + 1);
					double[] smaResults = movingaverage(slice, windSize);
					periodSma[i] = smaResults[smaResults.length - 1];

					if (i == (period - 1)) {
						periodEma[i] = periodSma[i];
					} else if (i > (period - 1)) {
						periodEma[i] = (test[i] - periodEma[i - 1]) * smoothingConstant + periodEma[i - 1];
						Object[] testData = (Object[]) selectData.get(k);
						List<Object> newList = new ArrayList<>(Arrays.asList(testData));
						newList.add(periodEma[i]);
						k++;
						intervatlistData.add(newList);
					}
					periodEma[i] = round(periodEma[i]);
					System.out.println(+i + "EMA IS:::" + periodEma[i]);
				}
				resultobjData.put("chartList", intervatlistData);
				sum = 0;
				period = 0;
				window.clear();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobjData;
	}

	public double[] movingaverage(double[] price, int period) throws Exception {
		double[] results;

		// ie: if you want 50 SMA then you need 50 data points
		if (price.length < period) {
			throw new Exception("Not enough data points, given data size less then the indicated period");
		}
		results = new double[price.length];
		int maxLength = price.length - period;
		for (int i = 0; i <= maxLength; i++) {
			results[(i + period - 1)] = round(
					(Arrays.stream(Arrays.copyOfRange(price, i, (i + period))).sum()) / period);
		}
		return results;

	}

	public double round(double value) {
		return round(value, 2);
	}

	public double round(double value, int numberOfDigitsAfterDecimalPoint) {
		BigDecimal bigDecimal = new BigDecimal(value);
		bigDecimal = bigDecimal.setScale(numberOfDigitsAfterDecimalPoint, BigDecimal.ROUND_HALF_UP);
		return bigDecimal.doubleValue();
	}

	public JSONObject getIndicatorDataObject(JSONObject framedDataObject, List columnKeys) {
		JSONObject indicatorObj = new JSONObject();
		try {
			if (framedDataObject != null && !framedDataObject.isEmpty() && columnKeys != null
					&& !columnKeys.isEmpty()) {
				String column = (String) columnKeys.get(0);
				if (column != null && !"".equalsIgnoreCase(column)) {
					JSONObject dataObj = (JSONObject) framedDataObject.get("dataObj");
					if (dataObj != null && !dataObj.isEmpty()) {
						Long indicatorVal = (long) dataObj.get(column);
						JSONObject gaugeObj = new JSONObject();
						indicatorObj.put("data", indicatorVal);
						JSONObject barObj = new JSONObject();
						barObj.put("color", "darkblue");
						gaugeObj.put("bar", barObj);
						indicatorObj.put("gauge", gaugeObj);

					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return indicatorObj;
	}

	@Transactional
	public JSONObject fetchHeatMapChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			long gratearVal = 0;
			JSONArray xAxisArr = new JSONArray();
			JSONArray yAxisArr = new JSONArray();
			JSONArray dataValuesArr = new JSONArray();
			String whereCondQuery = "";
			String groupByCond = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String aggregateColumns = request.getParameter("aggregateColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray colsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (aggregateColumns != null && !"".equalsIgnoreCase(aggregateColumns)
					&& !"null".equalsIgnoreCase(aggregateColumns)) {
				aggregateColsArr = (JSONArray) JSONValue.parse(aggregateColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			if (colsArr != null && !colsArr.isEmpty() && colsArr.size() > 2 && tablesArr != null
					&& !tablesArr.isEmpty()) {
				String tableName = (String) tablesArr.get(0);
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
				}
				String xAxisColumn = (String) colsArr.get(0);
				String yAxisColumn = (String) colsArr.get(1);
				String dataColumn = (String) colsArr.get(2);
				if (!(aggregateColsArr != null && !aggregateColsArr.isEmpty())) {
					dataColumn = "SUM(" + dataColumn + ")";
				}
				String xAxisQuery = "SELECT DISTINCT " + xAxisColumn + " FROM " + tableName + " " + whereCondQuery
						+ " ORDER BY " + xAxisColumn + " ASC";
				List selectData = access.sqlqueryWithParams(xAxisQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					String yAxisQuery = "SELECT DISTINCT " + yAxisColumn + " FROM " + tableName + " " + whereCondQuery
							+ " ORDER BY " + yAxisColumn + " ASC";
					List yAxisData = access.sqlqueryWithParams(yAxisQuery, new HashMap());
					if (yAxisData != null && !yAxisData.isEmpty()) {
						for (int k = 0; k < yAxisData.size(); k++) {
							yAxisArr.add(yAxisData.get(k));
						}
					}
					String xAxisCondQuery = "";
					for (int i = 0; i < selectData.size(); i++) {
						xAxisArr.add(selectData.get(i));
						if (!(whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
								&& !"null".equalsIgnoreCase(whereCondQuery))) {
							xAxisCondQuery = " WHERE " + xAxisColumn + " = '" + selectData.get(i) + "' ";
						} else {
							xAxisCondQuery = " AND " + xAxisColumn + " = '" + selectData.get(i) + "' ";
						}
						String dataQuery = "SELECT DISTINCT " + yAxisColumn + "," + dataColumn + " FROM " + tableName
								+ " " + whereCondQuery + xAxisCondQuery + " GROUP BY " + yAxisColumn + " ORDER BY "
								+ yAxisColumn + " ASC";
						List data = access.sqlqueryWithParams(dataQuery, new HashMap());
						if (data != null && !data.isEmpty()) {
							JSONArray dataAXisArr = new JSONArray();
							JSONObject dataAxisObj = new JSONObject();
							for (int j = 0; j < data.size(); j++) {
								Object[] objData = (Object[]) data.get(j);
								if (objData != null) {
									BigDecimal bigData = (BigDecimal) objData[1];
									if (bigData != null) {
										long longVal = bigData.longValue();
										if (gratearVal < longVal) {
											gratearVal = longVal;
										}
										dataAxisObj.put(objData[0], longVal);
									}
								}
							}
							for (int k = 0; k < yAxisArr.size(); k++) {
								String yAxisVal = (String) yAxisArr.get(k);
								if (yAxisVal != null && !"".equalsIgnoreCase(yAxisVal)) {
									if (dataAxisObj.containsKey(yAxisVal)) {
										dataAXisArr.add(dataAxisObj.get(yAxisVal));
									} else {
										dataAXisArr.add(0);
									}
								}
							}

							dataValuesArr.add(dataAXisArr);
						}
					}
				}
				chartObj.put("xAxis", xAxisArr);
				chartObj.put("yAxis", yAxisArr);
				chartObj.put("source", dataValuesArr);
				chartObj.put("dataPropObject", dataPropObj);
				chartObj.put("layout", layoutObj);
				chartObj.put("gratearVal", gratearVal);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public JSONObject linearRegression(HttpServletRequest request, JSONArray data, List selectData) {
		JSONObject lenearObj = new JSONObject();
		List<Integer> x = new ArrayList<Integer>();
		List<Integer> y = new ArrayList<Integer>();
		List lenearlistData = new ArrayList();
		List lenearlistData1 = new ArrayList();
		JSONArray indexarr = new JSONArray();
		try {
			int predictForDependentVariable = data.size();
			for (int i = 0; i < data.size(); i++) {
				BigDecimal testValue = (BigDecimal) data.get(i);

				x.add(i);
				if (testValue != null) {
					y.add(testValue.intValue());
				}
			}
			if (x.size() != y.size()) {
				throw new IllegalStateException("Must have equal X and Y data points");
			}
			Integer numberOfDataValues = x.size();
			List<Double> xSquared = x.stream().map(position -> Math.pow(position, 2)).collect(Collectors.toList());

			List<Integer> xMultipliedByY = IntStream.range(0, numberOfDataValues).map(i -> x.get(i) * y.get(i)).boxed()
					.collect(Collectors.toList());

			Integer xSummed = x.stream().reduce((prev, next) -> prev + next).get();

			Integer ySummed = y.stream().reduce((prev, next) -> prev + next).get();

			Double sumOfXSquared = xSquared.stream().reduce((prev, next) -> prev + next).get();

			Integer sumOfXMultipliedByY = xMultipliedByY.stream().reduce((prev, next) -> prev + next).get();

			int slopeNominator = numberOfDataValues * sumOfXMultipliedByY - ySummed * xSummed;
			Double slopeDenominator = numberOfDataValues * sumOfXSquared - Math.pow(xSummed, 2);
			Double slope = slopeNominator / slopeDenominator;

			double interceptNominator = ySummed - slope * xSummed;
			double interceptDenominator = numberOfDataValues;
			Double intercept = interceptNominator / interceptDenominator;
			double lenearvalue = (slope * predictForDependentVariable) + intercept;
			long lenearval = Math.round(lenearvalue);
			List<Object> newList1 = new ArrayList<>();
			for (int k = 0; k < selectData.size(); k++) {
				Object[] testData = (Object[]) selectData.get(k);
				List<Object> newList = new ArrayList<>(Arrays.asList(testData));
				String inputvalue = (String) newList.get(0);
				BigDecimal testValue = (BigDecimal) newList.get(1);
				lenearlistData.add(newList);
				if (selectData != null && k == selectData.size() - 1) {
					newList1.add("LNR");
					newList1.add(lenearval);
				}
			}

			lenearlistData.add(newList1);
			lenearObj.put("chartList", lenearlistData);
		} catch (Exception e) {
			e.getCause();
			e.printStackTrace();
		}
		return lenearObj;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String gridUpdateRecords(HttpServletRequest request, JSONObject newGridData, String baseTableName,
			String gridId) {
		String updateResult = "";
		try {
			try {
				baseTableName = (String) getViewTable(request, gridId);
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (newGridData != null && !newGridData.isEmpty()) {
				if (baseTableName != null && !"".equalsIgnoreCase(baseTableName)) {
					String updatequery = "UPDATE " + baseTableName
							+ " SET MASTER_COLUMN14=:MASTER_COLUMN14 WHERE RECORD_NO=:RECORD_NO";
					Map<String, Object> updateMap = new HashMap<>();
					updateMap.put("MASTER_COLUMN14", newGridData.get("MASTER_COLUMN14")); // chartId
					updateMap.put("RECORD_NO", newGridData.get("RECORD_NO")); // chartId
					System.out.println("updateMap:::" + updateMap);
					int updatecount = access.executeUpdateSQLNoAudit(updatequery, updateMap);
					if (updatecount != 0) {
						updateResult = "Updated Successfully";
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return updateResult;
	}

	@Transactional
	public String getViewTable(HttpServletRequest request, String gridid) {
		String tablename = "";
		try {
			String gridQuery = "SELECT  DAL_GRID.VIEW_TABLES FROM DAL_GRID WHERE GRID_ID=:GRID_ID AND ORGN_ID=:ORGN_ID";
			Map gridMap = new HashMap();
			gridMap.put("GRID_ID", gridid);
			gridMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
			List gridList = access.sqlqueryWithParams(gridQuery, gridMap);
			if (gridList != null && !gridList.isEmpty()) {
				tablename = (String) gridList.get(0);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tablename;
	}

	@Transactional
	public JSONObject fetchHeatMapEChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			long gratearVal = 0;
			JSONArray xAxisArr = new JSONArray();
			JSONArray yAxisArr = new JSONArray();
			JSONArray dataValuesArr = new JSONArray();
			String whereCondQuery = "";
			String groupByCond = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String aggregateColumns = request.getParameter("aggregateColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray colsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (aggregateColumns != null && !"".equalsIgnoreCase(aggregateColumns)
					&& !"null".equalsIgnoreCase(aggregateColumns)) {
				aggregateColsArr = (JSONArray) JSONValue.parse(aggregateColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			if (colsArr != null && !colsArr.isEmpty() && colsArr.size() > 2 && tablesArr != null
					&& !tablesArr.isEmpty()) {
				String tableName = (String) tablesArr.get(0);
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
				}
				String xAxisColumn = (String) colsArr.get(0);
				String yAxisColumn = (String) colsArr.get(1);
				String dataColumn = (String) colsArr.get(2);
				if (!(aggregateColsArr != null && !aggregateColsArr.isEmpty())) {
					if(dataColumn !=null && !"".equalsIgnoreCase(dataColumn) && dataColumn.contains("("))
					{
						dataColumn = dataColumn;
					}else {
					dataColumn = "SUM(" + dataColumn + ")";
					}
				}
				String xAxisQuery = "SELECT DISTINCT " + xAxisColumn + " FROM " + tableName + " " + whereCondQuery
						+ " ORDER BY " + xAxisColumn + " ASC";
				List selectData = access.sqlqueryWithParams(xAxisQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					String yAxisQuery = "SELECT DISTINCT " + yAxisColumn + " FROM " + tableName + " " + whereCondQuery
							+ " ORDER BY " + yAxisColumn + " ASC";
					List yAxisData = access.sqlqueryWithParams(yAxisQuery, new HashMap());
					if (yAxisData != null && !yAxisData.isEmpty()) {
						for (int k = 0; k < yAxisData.size(); k++) {
							yAxisArr.add(yAxisData.get(k));
						}
					}
					String xAxisCondQuery = "";
					for (int i = 0; i < selectData.size(); i++) {
						xAxisArr.add(selectData.get(i));
					}
					for (int y = 0; y < yAxisArr.size(); y++) {
						for (int z = 0; z < xAxisArr.size(); z++) {
							if (!(whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
									&& !"null".equalsIgnoreCase(whereCondQuery))) {
								xAxisCondQuery = " WHERE " + yAxisColumn + " = '" + yAxisArr.get(y) + "' AND "
										+ xAxisColumn + " = '" + xAxisArr.get(z) + "'";
							} else {
								xAxisCondQuery = " AND " + yAxisColumn + " = '" + yAxisArr.get(y) + "' AND "
										+ xAxisColumn + " = '" + xAxisArr.get(z) + "'";
							}
							String dataQuery = "SELECT DISTINCT " + dataColumn + " FROM " + tableName + " "
									+ whereCondQuery + xAxisCondQuery;
							List data = access.sqlqueryWithParams(dataQuery, new HashMap());
							if (data != null && !data.isEmpty()) {
								for (int j = 0; j < data.size(); j++) {
									JSONArray dataAXisArr = new JSONArray();
									Object objData = data.get(j);
									if (objData != null) {
										BigDecimal bigData = (BigDecimal) objData;
										if (bigData != null) {
											long longVal = bigData.longValue();
											if (gratearVal < longVal) {
												gratearVal = longVal;
											}
											dataAXisArr.add(y);
											dataAXisArr.add(z);
											dataAXisArr.add(longVal);
										} else {
											dataAXisArr.add(y);
											dataAXisArr.add(z);
											dataAXisArr.add(0);
										}
									}
									dataValuesArr.add(dataAXisArr);
								}
							}
						}
					}
				}
				chartObj.put("xAxis", xAxisArr);
				chartObj.put("yAxis", yAxisArr);
				chartObj.put("source", dataValuesArr);
				chartObj.put("dataPropObject", dataPropObj);
				chartObj.put("layout", layoutObj);
				chartObj.put("gratearVal", gratearVal);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	@Transactional
	public JSONObject fetchSunbrstEChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			JSONArray dataArr = new JSONArray();
			long gratearVal = 0;
			Double currencyConversionRate = null;
			JSONArray xAxisArr = new JSONArray();
			JSONArray yAxisArr = new JSONArray();
			JSONArray dataValuesArr = new JSONArray();
			String whereCondQuery = "";
			String groupByCond = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String aggregateColumns = request.getParameter("aggregateColumns");
			String tables = request.getParameter("tablesObj");
			String radioButtons = request.getParameter("radioButtons");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray colsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			if (radioButtons != null && !"".equalsIgnoreCase(radioButtons)) {
				chartObj.put("radioButtonStr", getradioButtonsStr(chartId, radioButtons));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}

			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			if (colsArr != null && !colsArr.isEmpty() && colsArr.size() > 2 && tablesArr != null
					&& !tablesArr.isEmpty()) {
				String tableName = (String) tablesArr.get(0);
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
				}
				String xAxisColumn = (String) colsArr.get(0);
				String yAxisColumn = (String) colsArr.get(1);
				String dataColumn = (String) colsArr.get(2);
				aggregateColsArr.add("SUM");
				aggregateColsArr.add("COUNT");
				aggregateColsArr.add("MAX");
				aggregateColsArr.add("MIN");
				aggregateColsArr.add("AVG");
				if (dataColumn != null && !"".equalsIgnoreCase(dataColumn)) {
					String dataColumnArr[] = dataColumn.split("\\(");
					String aggType = dataColumnArr[0];
					if (aggType != null && !"".equalsIgnoreCase(aggType) && !"null".equalsIgnoreCase(aggType)) {
						if (!(aggregateColsArr != null && !aggregateColsArr.isEmpty()
								&& aggregateColsArr.contains(aggType.toUpperCase()))) {
							dataColumn = "SUM(" + dataColumn + ")";
						}
					}
				}
				String currencyConversionEvent = request.getParameter("isCurrencyConversionEvent");
				boolean isCurrencyConversionEvent = false;
				if (currencyConversionEvent != null && !"".equalsIgnoreCase(currencyConversionEvent)
						&& !"null".equalsIgnoreCase(currencyConversionEvent)) {
					isCurrencyConversionEvent = Boolean.parseBoolean(currencyConversionEvent);
					currencyConversionRate = getCurrencyConversionRate(request);
				}
				String xAxisQuery = "SELECT DISTINCT " + xAxisColumn + " FROM " + tableName + " " + whereCondQuery
						+ " ORDER BY " + xAxisColumn + " ASC";
				List selectData = access.sqlqueryWithParams(xAxisQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					for (int j = 0; j < selectData.size(); j++) {
						String xAxisVal = (String) selectData.get(j);
						String xAxisCondQuery = "";
						if (!(whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
								&& !"null".equalsIgnoreCase(whereCondQuery))) {
							xAxisCondQuery += " WHERE " + xAxisColumn + " = '" + xAxisVal + "'";
						} else {
							xAxisCondQuery = " AND " + xAxisColumn + " = '" + xAxisVal + "'";
						}
						String childQuery = "SELECT DISTINCT " + yAxisColumn + "," + dataColumn + " FROM " + tableName
								+ " " + whereCondQuery + xAxisCondQuery + " GROUP BY " + yAxisColumn;
						List childData = access.sqlqueryWithParams(childQuery, new HashMap());
						JSONArray childArr = new JSONArray();
						long longVal = 0;
						for (int k = 0; k < childData.size(); k++) {
							Object[] objData = (Object[]) childData.get(k);
							if (objData != null) {
								JSONObject childObj = new JSONObject();
								childObj.put("name", objData[0]);
								if (objData[1] != null && currencyConversionRate != null && isCurrencyConversionEvent) {
									double chartValueIndouble = (Double) dashboardutils
											.getRequiredObjectTypeFromObject(objData[1], "BigDecimal", "Double");
									double convertedCurrencyValue = chartValueIndouble * currencyConversionRate;
									childObj.put("value", convertedCurrencyValue);
									longVal += (long) convertedCurrencyValue;
								} else {
									childObj.put("value", objData[1]);
									longVal += (objData[1] != null ? ((BigDecimal) objData[1]).longValue() : 0);
								}
								childArr.add(childObj);
							}
						}
						JSONObject dataObj = new JSONObject();
						dataObj.put("name", xAxisVal);
						dataObj.put("value", longVal);
						dataObj.put("children", childArr);
						dataArr.add(dataObj);
					}
				}
				chartObj.put("data", dataArr);
				chartObj.put("tableName", tableName);
				chartObj.put("dataPropObject", dataPropObj);
				chartObj.put("layout", layoutObj);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	@Transactional
	public JSONObject fetchGeoChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			String region = "";
			JSONArray dataArr = new JSONArray();
			long gratearVal = 0;
			JSONArray xAxisArr = new JSONArray();
			JSONArray yAxisArr = new JSONArray();
			JSONArray dataValuesArr = new JSONArray();
			String whereCondQuery = "";
			String groupByCond = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String aggregateColumns = request.getParameter("aggregateColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray colsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (aggregateColumns != null && !"".equalsIgnoreCase(aggregateColumns)
					&& !"null".equalsIgnoreCase(aggregateColumns)) {
				aggregateColsArr = (JSONArray) JSONValue.parse(aggregateColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			JSONArray labelsArr = new JSONArray();
			labelsArr.add("number:Latitude");
			labelsArr.add("number:Longitude");
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
							if (!groupByCond.contains("GROUP BY")) {
								groupByCond += " GROUP BY " + columnName + ",";
							} else {
								groupByCond += columnName + ",";
							}
							if (columnName != null && !"".equalsIgnoreCase(columnName) && columnName.contains(".")) {
								String cols[] = columnName.split("\\.");
								labelsArr.add("string:" + cols[1]);
							} else {
								labelsArr.add("string:" + columnName);
							}
						}
					}
				}
			}
			String columnName = "";
			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						columnName = (String) valueColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
							if (columnName != null && !"".equalsIgnoreCase(columnName) && columnName.contains(".")) {
								String cols[] = columnName.split("\\.");
								String col = cols[1];
								if (col != null && !"".equalsIgnoreCase(col) && col.contains(")")) {
									col = col.replace(")", "");
								}
								labelsArr.add("number:" + col);
							} else {
								labelsArr.add("number:" + columnName);
							}

						}
					}
				}
			}
			chartObj.put("labelsArr", labelsArr);
			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			if (colsArr != null && !colsArr.isEmpty() && tablesArr != null && !tablesArr.isEmpty()) {
				String tableName = (String) tablesArr.get(0);
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
				}
				String selectQuery = "";
				for (int c = 0; c < colsArr.size(); c++) {
					selectQuery += colsArr.get(c);
					if (c != colsArr.size() - 1) {
						selectQuery += ",";
					}
				}
				if (groupByCond != null && !"".equalsIgnoreCase(groupByCond)) {
					groupByCond = new PilogUtilities().trimChar(groupByCond, ',');
				} else {
					groupByCond = "";
				}
				selectQuery = "SELECT DISTINCT " + selectQuery + " FROM " + tableName + " " + whereCondQuery
						+ groupByCond;
				List selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
				HashSet regionSet = new HashSet();  
				if (selectData != null && !selectData.isEmpty()) {
					for (int j = 0; j < selectData.size(); j++) {
						Object[] objData = (Object[]) selectData.get(j);
						if (objData != null) {
							JSONArray geoDataArr = new JSONArray();
							String location = (String) objData[0];
							String geoLatQuery = "SELECT LATITUDE,LONGITUDE,COUNTRY_CODE FROM B_VISU_LAT_LONG WHERE LOCATION ='"
									+ location + "'";
							List geoData = access.sqlqueryWithParams(geoLatQuery, new HashMap());
							if (geoData != null && !geoData.isEmpty()) {
								Object[] geoObjData = (Object[]) geoData.get(0);
								geoDataArr.add(geoObjData[0]);
								geoDataArr.add(geoObjData[1]);
								regionSet.add(geoObjData[2]);
								for (int k = 0; k < objData.length; k++) {
									geoDataArr.add(objData[k]);
								}
								dataArr.add(geoDataArr); 
							}
							
							
						}
					}
				}
				
				 
				String CountQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName + "";
				List countData = access.sqlqueryWithParams(CountQuery, new HashMap());
				chartObj.put("totalValue", countData);
				chartObj.put("data", dataArr);
				chartObj.put("dataPropObject", dataPropObj);
				chartObj.put("layout", layoutObj);
				if (regionSet != null && !regionSet.isEmpty()) {
					if (regionSet.size() > 1) {  
						region = "world";
					} else {
						region = (String) regionSet.iterator().next();
					}

				}
				chartObj.put("region", region);      
				chartObj.put("chartCOnfigObjStr", chartConfigObj);
				chartObj.put("tableName", tableName);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public JSONObject getBarRotationDataObject(JSONObject dataObj, List columnKeys) {
		JSONObject barRotationDataObj = new JSONObject();
		try {
			JSONObject xAxisObj = new JSONObject();
			JSONArray xAxisArr = new JSONArray();
			JSONObject axisTickObj = new JSONObject();
			axisTickObj.put("show", false);
			xAxisObj.put("type", "category");
			xAxisObj.put("axisTick", axisTickObj);
			xAxisObj.put("data", dataObj.get(columnKeys.get(0)));
			xAxisArr.add(xAxisObj);

			JSONObject yAxisObj = new JSONObject();
			JSONArray yAxisArr = new JSONArray();
			yAxisObj.put("type", "value");
			yAxisArr.add(yAxisObj);

			JSONArray legendArr = new JSONArray();
			JSONArray seriesArr = new JSONArray();
			for (int i = 1; i < columnKeys.size(); i++) {
				JSONObject seriesObj = new JSONObject();
				seriesObj.put("name", columnKeys.get(i));
				seriesObj.put("type", "bar");
				if (i == 1) {
					// seriesObj.put("barGap", 0);
				}
				seriesObj.put("barWidth", 60);
				seriesObj.put("label", new JSONObject());
				JSONObject emphasisObj = new JSONObject();
				emphasisObj.put("focus", "series");
				seriesObj.put("emphasis", emphasisObj);
				seriesObj.put("data", dataObj.get(columnKeys.get(i)));
				seriesArr.add(seriesObj);
				legendArr.add(columnKeys.get(i));
			}
			JSONObject legendObj = new JSONObject();
			legendObj.put("data", legendArr);
			barRotationDataObj.put("xAxis", xAxisObj);
			barRotationDataObj.put("yAxis", yAxisObj);
			barRotationDataObj.put("series", seriesArr);
			barRotationDataObj.put("legend", legendObj);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return barRotationDataObj;
	}

	@Transactional
	public JSONObject saveHomeChartsColorsData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		int updatecount = 0;
		try {
			long pnVal = 0;
			long tnVal = 0;
			JSONArray clrsArr = new JSONArray();
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String pn = request.getParameter("pn");
			String tn = request.getParameter("tn");
			String colotrs = request.getParameter("colotrs");
			if (colotrs != null && !"".equalsIgnoreCase(colotrs) && !"".equalsIgnoreCase(colotrs) && chartType != null
					&& !"".equalsIgnoreCase(chartType) && !"lines".equalsIgnoreCase(chartType)) {
				clrsArr = (JSONArray) JSONValue.parse(colotrs);
			}
			if (pn != null && !"".equalsIgnoreCase(pn)) {
				pnVal = Integer.parseInt(pn);
			}
			if (tn != null && !"".equalsIgnoreCase(tn)) {
				tnVal = Integer.parseInt(tn);
			}
			String selectQuery = "SELECT VISUALIZE_CUST_COL8 FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID AND CREATE_BY =:CREATE_BY AND CHART_TYPE =:CHART_TYPE";
			Map<String, Object> selectMap = new HashMap<>();
			selectMap.put("CHART_ID", chartId);
			selectMap.put("CREATE_BY", (String) request.getSession(false).getAttribute("ssUsername")); // chartId
			selectMap.put("CHART_TYPE", chartType);
			System.out.println("selectMap:::" + selectMap);
			List listData = access.sqlqueryWithParams(selectQuery, selectMap);
			if (listData != null && !listData.isEmpty()) {
				String clrsDataStr = (String) listData.get(0);
				if (clrsDataStr != null && !"".equalsIgnoreCase(clrsDataStr)) {
					JSONObject clrsDataObj = (JSONObject) JSONValue.parse(clrsDataStr);
					if (clrsDataObj != null && !clrsDataObj.isEmpty()) {
						boolean flag = false;
						JSONArray pnArr = (JSONArray) clrsDataObj.get("pn");
						JSONArray tnArr = (JSONArray) clrsDataObj.get("tn");
						JSONArray clrs = (JSONArray) clrsDataObj.get("clrs");
						if (pnArr != null && !pnArr.isEmpty()) {
							for (int j = 0; j < pnArr.size(); j++) {
								long pnNum = (long) pnArr.get(j);
								long tnNum = (long) tnArr.get(j);
								if (chartType != null && !"".equalsIgnoreCase(chartType)
										&& "lines".equalsIgnoreCase(chartType)) {
									if (tnVal == tnNum) {
										flag = true;
										clrs.remove(j);
										clrs.add(j, colotrs);
										break;
									}
								} else {
									if (pnVal == pnNum && tnVal == tnNum) {
										flag = true;
										if (chartType != null && !"".equalsIgnoreCase(chartType)
												&& ("pie".equalsIgnoreCase(chartType)
														|| "donut".equalsIgnoreCase(chartType))) {
											clrs = clrsArr;
										} else {
											clrs.remove(j);
											clrs.add(j, colotrs);
										}
										break;
									}
								}

							}

							if (!flag) {
								pnArr.add(pnVal);
								tnArr.add(tnVal);
								if (chartType != null && !"".equalsIgnoreCase(chartType)
										&& ("pie".equalsIgnoreCase(chartType) || "donut".equalsIgnoreCase(chartType))) {
									clrs = clrsArr;
								} else {
									clrs.add(colotrs);
								}

							}
						}
						JSONObject clrsObj = new JSONObject();
						if (chartType != null && !"".equalsIgnoreCase(chartType)
								&& "scatterpolar".equalsIgnoreCase(chartType)) {
							JSONArray radarClrs = new JSONArray();
							radarClrs.add(colotrs);
							clrsObj.put("clrs", radarClrs);
						} else {

							clrsObj.put("clrs", clrs);
							clrsObj.put("tn", tnArr);
							clrsObj.put("pn", pnArr);
						}

						String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET VISUALIZE_CUST_COL8 =:VISUALIZE_CUST_COL8 WHERE CHART_ID =:CHART_ID AND CHART_TYPE =:CHART_TYPE";
						Map updateMap = new HashMap<>();
						updateMap.put("VISUALIZE_CUST_COL8", clrsObj.toString());
						updateMap.put("CHART_ID", chartId);
						updateMap.put("CHART_TYPE", chartType);
						System.out.println("updateMap:::" + updateMap);
						updatecount = access.executeUpdateSQLNoAudit(updateQuery, updateMap);
					}
				} else {
					JSONArray pnArr = new JSONArray();
					JSONArray tnArr = new JSONArray();
					JSONArray clrs = new JSONArray();
					pnArr.add(pnVal);
					tnArr.add(tnVal);
					if (chartType != null && !"".equalsIgnoreCase(chartType)
							&& ("pie".equalsIgnoreCase(chartType) || "donut".equalsIgnoreCase(chartType))) {
						clrs = clrsArr;
					} else {
						clrs.add(colotrs);
					}
					JSONObject clrsObj = new JSONObject();
					clrsObj.put("clrs", clrs);
					clrsObj.put("tn", tnArr);
					clrsObj.put("pn", pnArr);

					String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET VISUALIZE_CUST_COL8 =:VISUALIZE_CUST_COL8 WHERE CHART_ID =:CHART_ID AND CHART_TYPE =:CHART_TYPE";
					Map updateMap = new HashMap<>();
					updateMap.put("VISUALIZE_CUST_COL8", clrsObj.toString());
					updateMap.put("CHART_ID", chartId);
					updateMap.put("CHART_TYPE", chartType);
					System.out.println("updateMap:::" + updateMap);
					updatecount = access.executeUpdateSQLNoAudit(updateQuery, updateMap);

				}
			} else {
				JSONArray pnArr = new JSONArray();
				JSONArray tnArr = new JSONArray();
				JSONArray clrs = new JSONArray();
				pnArr.add(pnVal);
				tnArr.add(tnVal);
				if (chartType != null && !"".equalsIgnoreCase(chartType)
						&& ("pie".equalsIgnoreCase(chartType) || "donut".equalsIgnoreCase(chartType))) {
					clrs = clrsArr;
				} else {
					clrs.add(colotrs);
				}
				JSONObject clrsObj = new JSONObject();
				clrsObj.put("clrs", clrs);
				clrsObj.put("tn", tnArr);
				clrsObj.put("pn", pnArr);

				String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET VISUALIZE_CUST_COL8 =:VISUALIZE_CUST_COL8 WHERE CHART_ID =:CHART_ID AND CHART_TYPE =:CHART_TYPE";
				Map updateMap = new HashMap<>();
				updateMap.put("VISUALIZE_CUST_COL8", clrsObj.toString());
				updateMap.put("CHART_ID", chartId);
				updateMap.put("CHART_TYPE", chartType);
				System.out.println("updateMap:::" + updateMap);
				updatecount = access.executeUpdateSQLNoAudit(updateQuery, updateMap);

			}
			if (updatecount != 0) {
				resultObj.put("Message", "Colors Appiled successFully");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}

	@Transactional
	public JSONObject getSurveyHomeCharts(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			JSONArray tablesArr = new JSONArray();
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String role = (String) request.getSession(false).getAttribute("ssRole");
			String dashBoardName = (String) request.getParameter("dashBoardName");
			String sureveyFilterArr = (String) request.getParameter("sureveyFilterArr");

			if (dashBoardName != null && !"".equalsIgnoreCase(dashBoardName)
					&& !"null".equalsIgnoreCase(dashBoardName)) {

				String selectquery = "SELECT " + "X_AXIS_VALUE, "// 0
						+ "Y_AXIS_VALUE,"// 1
						+ "CHART_TYPE,"// 2
						+ "TABLE_NAME,"// 3
						+ "CHART_ID,"// 4
						+ "AGGRIGATE_COLUMNS, "// 5
						+ "FILTER_CONDITION, "// 6
						+ "CHART_PROPERTIES, "// 7
						+ "CHART_CONFIG_OBJECT, "// 8
						+ "VISUALIZE_CUST_COL10, "// 9
						+ "CHART_TITTLE, " // 10
						+ "VISUALIZE_CUST_COL8, " // 11
						+ "COMBO_VALUE " // 12
						+ "FROM " + "O_RECORD_VISUALIZATION " + "WHERE CREATE_BY =:CREATE_BY "
						+ "AND DASHBORD_NAME =:DASHBORD_NAME " + "AND ROLE_ID =:ROLE_ID "
						+ "ORDER BY CHART_SEQUENCE_NO";
				HashMap datamap = new HashMap();
				datamap.put("CREATE_BY", userName);
				datamap.put("DASHBORD_NAME", dashBoardName);
				datamap.put("ROLE_ID", role);

				List datalist = access.sqlqueryWithParams(selectquery, datamap);
				if (datalist != null && !datalist.isEmpty()) {
					for (int i = 0; i < datalist.size(); i++) {
						Object[] rowData = (Object[]) datalist.get(i);
						JSONObject dataobj = new JSONObject();
						dataobj.put("xAxix", rowData[0]);
						dataobj.put("yAxix", rowData[1]);
						dataobj.put("type", rowData[2]);
						dataobj.put("table", rowData[3]);
						dataobj.put("chartid", rowData[4]);
						dataobj.put("aggColumnName", rowData[5]);
						dataobj.put("filterCondition", rowData[6]);
						dataobj.put("chartPropObj", rowData[7]);
						dataobj.put("chartConfigObj", rowData[8]);
						dataobj.put("labelLegend", rowData[9]);
						dataobj.put("Lebel", rowData[10]);
						dataobj.put("colorsObj", rowData[11]);
						dataobj.put("comboValue", rowData[12]);
						dataobj.put("sureveyFilterArr", sureveyFilterArr);
						dataarr.add(dataobj);
					}
					tabledataobj.put("dataarr", dataarr);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public JSONObject getHomeChartHeaderFilterForm(HttpServletRequest request) {
		JSONObject chartSlicerData = new JSONObject();
		Connection connection = null;
		try {
			String result = "";
			String columnStr = "";
			String dashBoardName = request.getParameter("chartDropDownVal");
			String resultStr = "<div id='HomeSlicerColumndataId' class = 'HomeSlicerColumndataClass'>"
					+ "<div id=\"VisualizeBIHomeSlicerColumns\"></div>"
					+ "<div id=\"visualizeChartHomeSlicerData\" class=\"visualizeChartHomeSlicerClass\"></div>"
					+ "</div>";
			String tableQuery = "SELECT DISTINCT TABLE_NAME FROM O_RECORD_VISUALIZATION WHERE ROLE_ID =:ROLE_ID AND ORGN_ID=:ORGN_ID AND CHART_TYPE NOT IN('CARD','FILTER','COMPARE_FILTER') AND  DASHBORD_NAME =:DASHBORD_NAME";
			Map tableMap = new HashMap();
//            tableMap.put("CREATE_BY", request.getSession(false).getAttribute("ssUsername"));
			tableMap.put("DASHBORD_NAME", dashBoardName);
			tableMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
			tableMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
			List listData = access.sqlqueryWithParams(tableQuery, tableMap);
			if (listData != null && !listData.isEmpty()) {
				for (int j = 0; j < listData.size(); j++) {
					String tableName = (String) listData.get(j);
					Class.forName(dataBaseDriver);
					connection = DriverManager.getConnection(dbURL, userName, password);
					Statement statement = connection.createStatement();
					ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
					ResultSetMetaData metadata = results.getMetaData();
					int columnCount = metadata.getColumnCount();
					if (columnCount > 0) {
						result += "<div  class='visionVisualizeHomeChartTableToggleClass'>";
						result += "<div class='visionVisualizeChartTableClass'><img src=\"images/nextrightarrow.png\" id=\"VisionImageVisualizationTableId\" class=\"VisionImageVisualizationHomeTableClass\" title=\"Show/Hide Table\"/>"
								+ "<img src=\"images/GridDB.png\" class=\"VisionImageVisualizationTableImageClass\" title=\"Show/Hide Table\"/><h6>"
								+ tableName + "</h6></div>";
						result += "<ul class='visionVisualizationDragColumns'>";
						result += "<div class='homechartSlicerColumnsDiv'>";
						for (int i = 1; i <= columnCount; i++) {
							String columnName = metadata.getColumnName(i);
							String columnType = metadata.getColumnTypeName(i);
							String id = tableName + "_" + columnName;
							String addedColName = tableName + "." + columnName;
							if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "NUMBER".equalsIgnoreCase(columnType)) {
								result += "<li id=\"" + id
										+ "liId\"><img src=\"images/sigma-icon.jpg\" class=\"VisionImageVisualizationHomeTableClass\"/><span class='columnNameIS'>"
										+ columnName + "</span>"
										+ "<span class='columnAddImg'><img src='images/image2vector.svg' class='addcolumnIcon' onclick=\"dashBoardHeaderFilter('"
										+ id + "','" + addedColName + "')\"/></span>" + "</li>";
							} else if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "DATE".equalsIgnoreCase(columnType)) {
								result += "<li id=\"" + id
										+ "liId\"><img src=\"images/calendar.svg\" class=\"VisionImageVisualizationHomeTableClass\"/><span class='columnNameIS'>"
										+ columnName + "</span>"
										+ "<span class='columnAddImg'><img src='images/image2vector.svg' class='addcolumnIcon' onclick=\"dashBoardHeaderFilter('"
										+ id + "','" + addedColName + "')\"/></span>" + "</li>";
							} else {
								result += "<li id=\"" + id + "liId\"><span class='columnNameIS'>" + columnName
										+ "</span>"
										+ "<span class='columnAddImg'><img src='images/image2vector.svg' class='addcolumnIcon' onclick=\"dashBoardHeaderFilter('"
										+ id + "','" + addedColName + "')\"/></span>" + "</li>";
							}
						}
						result += "</div></ul>";
						result += "</div>";
					}

				}
				columnStr += "<ul class='visionVisualizationDragColumns1'>";
				columnStr += "<div class='homefilterColumnsDiv'>";
				String filtercolQUery = "SELECT FILTER_COLUMN FROM O_RECORD_VISUALIZATION WHERE  CHART_TYPE =:CHART_TYPE AND DASHBORD_NAME =:DASHBORD_NAME AND ROLE_ID =:ROLE_ID AND ORGN_ID=:ORGN_ID";
				Map filterColMap = new HashMap();
//                filterColMap.put("CREATE_BY", request.getSession(false).getAttribute("ssUsername"));
				filterColMap.put("CHART_TYPE", "FILTER");
				filterColMap.put("DASHBORD_NAME", dashBoardName);
				filterColMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
				filterColMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
				List filterColumn = access.sqlqueryWithParams(filtercolQUery, filterColMap);
				chartSlicerData.put("result", result);
				chartSlicerData.put("resultStr", resultStr);
				if (filterColumn != null && !filterColumn.isEmpty()) {
					for (int i = 0; i < filterColumn.size(); i++) {
						String filteredColumn = (String) filterColumn.get(i);
						String[] columnArr = filteredColumn.split("\\,");
						for (int j = 0; j < columnArr.length; j++) {
							String tableAttr = columnArr[j];
							String tableArr[] = tableAttr.split("\\.");
							String id = tableArr[0] + "_" + tableArr[1];
							columnStr += "<li id=\"" + id + "SavedliId\">" + tableAttr
									+ "<img src='images/close_white.png'  class='VisualizeColumnCancelClass' onclick=deleteHeaderFilterColumns(\""
									+ id + "SavedliId\")  title='Close Chart'/></li>";
						}
					}
				}
				columnStr += "</div>" + "</ul>";
				chartSlicerData.put("columnStr", columnStr);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartSlicerData;
	}

	@Transactional
	public String updteFilterColumn(HttpServletRequest request) {
//        JSONObject resultObj = new JSONObject()
		String message = "";
		JSONArray Columnarr = new JSONArray();

		try {
			String dashbordname = request.getParameter("dashbordName");
			String username = (String) request.getSession(false).getAttribute("ssUsername");
			String roleId = (String) request.getSession(false).getAttribute("ssRole");
			String OrgnId = (String) request.getSession(false).getAttribute("ssOrgId");
			String Columndata = request.getParameter("Columndata");
			if (Columndata != null && !"".equalsIgnoreCase(Columndata) && !"null".equalsIgnoreCase(Columndata)) {
				Columnarr = (JSONArray) JSONValue.parse(Columndata);
			}
			String ColumnName = "";
			for (int i = 0; i < Columnarr.size(); i++) {
				String columnName = (String) Columnarr.get(i);
				if (columnName != null && !"".equalsIgnoreCase(columnName)) {
					ColumnName += columnName;
					if (i != Columnarr.size() - 1) {
						ColumnName += ",";
					}
				}
			}
			int recordCount = 0;
			String selectQuery = "SELECT * FROM O_RECORD_VISUALIZATION WHERE CHART_TYPE=:CHART_TYPE AND DASHBORD_NAME=:DASHBORD_NAME AND ROLE_ID=:ROLE_ID AND ORGN_ID=:ORGN_ID";
			Map mapData = new HashMap();
			mapData.put("CHART_TYPE", "FILTER");
			mapData.put("DASHBORD_NAME", dashbordname);
			mapData.put("ROLE_ID", roleId);
			mapData.put("ORGN_ID", OrgnId);
			List listData = access.sqlqueryWithParams(selectQuery, mapData);
			if (listData != null && !listData.isEmpty()) {
				recordCount = listData.size();
			}
			if (recordCount == 0) {
				String insertQuery = "INSERT INTO O_RECORD_VISUALIZATION(CHART_TYPE,FILTER_COLUMN,DASHBORD_NAME,ORGN_ID,ROLE_ID,CREATE_BY,EDIT_BY)"
						+ " Values(?,?,?,?,?,?,?)";
				Map<Integer, Object> insertMap = new HashMap<>();
				insertMap.put(1, "FILTER");
				insertMap.put(2, ColumnName);
				insertMap.put(3, dashbordname);
				insertMap.put(4, OrgnId);
				insertMap.put(5, roleId);
				insertMap.put(6, username);
				insertMap.put(7, username);
				int count = access.executeNativeUpdateSQLWithSimpleParamsNoAudit(insertQuery, insertMap);
				if (count > 0) {
					message = "Column Inserted Successfully";
				}
			} else {
				String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET FILTER_COLUMN=:FILTER_COLUMN WHERE CHART_TYPE=:CHART_TYPE AND DASHBORD_NAME=:DASHBORD_NAME AND ROLE_ID=:ROLE_ID AND ORGN_ID=:ORGN_ID";
				Map updateMap = new HashMap();
				updateMap.put("FILTER_COLUMN", ColumnName);
				updateMap.put("CHART_TYPE", "FILTER");
				updateMap.put("DASHBORD_NAME", dashbordname);
				updateMap.put("ROLE_ID", roleId);
				updateMap.put("ORGN_ID", OrgnId);
				int count = access.executeUpdateSQL(updateQuery, updateMap);
				if (count > 0) {
					message = "Column Updated Successfully";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	@Transactional
	public String saveChartRadioButtonColumns(HttpServletRequest request) {
//        JSONObject resultObj = new JSONObject()
		String message = "";
		JSONArray columnArr = new JSONArray();

		try {
			String dashbordname = request.getParameter("dashbordName");
			String chartType = request.getParameter("chartType");
			String chartId = request.getParameter("chartId");
			String username = (String) request.getSession(false).getAttribute("ssUsername");
			String roleId = (String) request.getSession(false).getAttribute("ssRole");
			String OrgnId = (String) request.getSession(false).getAttribute("ssOrgId");
			String columnData = request.getParameter("columnData");
			if (columnData != null && !"".equalsIgnoreCase(columnData) && !"null".equalsIgnoreCase(columnData)) {
				columnArr = (JSONArray) JSONValue.parse(columnData);
			}
			String ColumnName = "";
			if (columnArr != null && !columnArr.isEmpty()) {
				for (int i = 0; i < columnArr.size(); i++) {
					String columnName = (String) columnArr.get(i);
					if (columnName != null && !"".equalsIgnoreCase(columnName)) {
						ColumnName += columnName;
						if (i != columnArr.size() - 1) {
							ColumnName += ",";
						}
					}
				}

				String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET VISUALIZE_CUST_COL7=:VISUALIZE_CUST_COL7 WHERE CHART_ID=:CHART_ID AND CHART_TYPE=:CHART_TYPE AND DASHBORD_NAME=:DASHBORD_NAME AND ROLE_ID=:ROLE_ID AND ORGN_ID=:ORGN_ID";
				Map updateMap = new HashMap();
				updateMap.put("VISUALIZE_CUST_COL7", ColumnName);
				updateMap.put("CHART_ID", chartId);
				updateMap.put("CHART_TYPE", chartType);
				updateMap.put("DASHBORD_NAME", dashbordname);
				updateMap.put("ROLE_ID", roleId);
				updateMap.put("ORGN_ID", OrgnId);
				int count = access.executeUpdateSQL(updateQuery, updateMap);
				if (count > 0) {
					message = "Column Updated Successfully";
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	@Transactional
	public JSONObject getCompareChartDataList(HttpServletRequest request) {
		JSONObject chartListObj = new JSONObject();
		try {
			boolean flag = false;
			String selectQuery = "";
			String whereCondQuery = "";
			String groupByCond = "";
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String chartPorpObj = request.getParameter("chartPorpObj");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String compareChartFilters = request.getParameter("compareChartFilters");
			List<String> columnKeys = new ArrayList<>();
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject compareChartFiltersObj = new JSONObject();
			if (compareChartFilters != null && !"".equalsIgnoreCase(compareChartFilters)
					&& !"null".equalsIgnoreCase(compareChartFilters)) {
				compareChartFiltersObj = (JSONObject) JSONValue.parse(compareChartFilters);
			}
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							String[] columns = columnName.split(",");
							if (columns != null && columns.length > 0) {
								for (int j = 0; j < columns.length; j++) {
									String column = columns[j];
									String[] filteredColumnnameArr = column.split("\\.");
									String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
									if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
											&& !"null".equalsIgnoreCase(filteredColumnname)) {
										filteredColumnname = filteredColumnname.replaceAll("_", " ");
									}
									columnKeys.add(filteredColumnname);
									selectQuery += " " + column + ", ";
									groupByCond += column + ", ";

								}
							}
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						String[] filteredColumnnameArr = columnName.split("\\.");
						String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
						if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
								&& !"null".equalsIgnoreCase(filteredColumnname)) {
							filteredColumnname = filteredColumnname.replaceAll("_", " ");
						}
						columnKeys.add(filteredColumnname + "ASCOL" + i);
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {
							selectQuery += " " + columnName + " AS COL" + i + " ,";
							flag = true;
						} else {
							selectQuery += " " + columnName + " AS COL" + i + " , ";
							groupByCond += columnName;
							if (i < valuesColsArr.size() - 1) {
								groupByCond += ",";
							}
						}
					}
				}

				if (chartType != null && !"".equalsIgnoreCase(chartType)
						&& ("indicator".equalsIgnoreCase(chartType) || "Card".equalsIgnoreCase(chartType))) {
					groupByCond = "";
				} else if (!flag) {
					groupByCond = "";
				} else if (groupByCond != null && !"".equalsIgnoreCase(groupByCond)) {
					groupByCond = new PilogUtilities().trimChar(groupByCond, ',');
					groupByCond = " GROUP BY " + groupByCond;
				}

			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			String compareQuery = selectQuery;
			if (selectQuery != null && !"".equalsIgnoreCase(selectQuery) && tablesArr != null && !tablesArr.isEmpty()) {
				String tableName = (String) tablesArr.get(0);
				selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
				compareQuery = new PilogUtilities().trimChar(compareQuery, ',');
				String chartFilterCond1 = "";
				String chartFilterCond2 = "";
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
					if (compareChartFiltersObj != null && !compareChartFiltersObj.isEmpty()) {
						String filterCond1 = "";
						JSONArray chartFilterArr1 = (JSONArray) compareChartFiltersObj.get("chart1");
						filterCond1 += chartFilterArr1.stream().filter(params -> (params != null))
								.map(paramFilterData -> buildCondition((JSONObject) paramFilterData, request))
								.collect(Collectors.joining(" AND "));
						if (filterCond1 != null && !"".equalsIgnoreCase(filterCond1)) {
							filterCond1 = cloudUtills.trimAND(filterCond1);
						}
						if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
								&& !"null".equalsIgnoreCase(whereCondQuery)) {
							chartFilterCond1 = " WHERE " + whereCondQuery + " AND " + filterCond1;
						} else {
							chartFilterCond1 = " WHERE " + filterCond1;
						}
						JSONArray chartFilterArr2 = (JSONArray) compareChartFiltersObj.get("chart2");
						String filterCond2 = "";
						filterCond2 += chartFilterArr2.stream().filter(params -> (params != null))
								.map(paramFilterData -> buildCondition((JSONObject) paramFilterData, request))
								.collect(Collectors.joining(" AND "));
						if (filterCond2 != null && !"".equalsIgnoreCase(filterCond2)) {
							filterCond2 = cloudUtills.trimAND(filterCond2);
						}
						if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
								&& !"null".equalsIgnoreCase(whereCondQuery)) {
							chartFilterCond2 = " WHERE " + whereCondQuery + " AND " + filterCond2;
						} else {
							chartFilterCond2 = " WHERE " + filterCond2;
						}
					}
					selectQuery = "SELECT " + selectQuery + " " + tableName + chartFilterCond1 + groupByCond;
					compareQuery = "SELECT " + compareQuery + " " + tableName + chartFilterCond2 + groupByCond;
					System.out.println("selectQuery :::" + selectQuery);
				} else {
					if (compareChartFiltersObj != null && !compareChartFiltersObj.isEmpty()) {
						String filterCond1 = "";
						JSONArray chartFilterArr1 = (JSONArray) compareChartFiltersObj.get("chart1");
						filterCond1 += chartFilterArr1.stream().filter(params -> (params != null))
								.map(paramFilterData -> buildCondition((JSONObject) paramFilterData, request))
								.collect(Collectors.joining(" AND "));
						if (filterCond1 != null && !"".equalsIgnoreCase(filterCond1)) {
							filterCond1 = cloudUtills.trimAND(filterCond1);
						}
						if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
								&& !"null".equalsIgnoreCase(whereCondQuery)) {
							chartFilterCond1 = " WHERE " + whereCondQuery + " AND " + filterCond1;
						} else {
							chartFilterCond1 = " WHERE " + filterCond1;
						}
						JSONArray chartFilterArr2 = (JSONArray) compareChartFiltersObj.get("chart2");
						String filterCond2 = "";
						filterCond2 += chartFilterArr2.stream().filter(params -> (params != null))
								.map(paramFilterData -> buildCondition((JSONObject) paramFilterData, request))
								.collect(Collectors.joining(" AND "));
						if (filterCond2 != null && !"".equalsIgnoreCase(filterCond2)) {
							filterCond2 = cloudUtills.trimAND(filterCond2);
						}
						if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
								&& !"null".equalsIgnoreCase(whereCondQuery)) {
							chartFilterCond2 = " WHERE " + whereCondQuery + " AND " + filterCond2;
						} else {
							chartFilterCond2 = " WHERE " + filterCond2;
						}
					}
					selectQuery = "SELECT " + selectQuery + " FROM " + tableName + chartFilterCond1 + groupByCond;
					compareQuery = "SELECT " + compareQuery + " FROM " + tableName + chartFilterCond2 + groupByCond;
					System.out.println("selectQuery :::" + selectQuery);
					System.out.println("compareQuery :::" + compareQuery);
				}
			}
			List allDataObj = new ArrayList();
			List<Object[]> selectData = access.sqlqueryWithParams(selectQuery, new HashMap());
			List<Object[]> compareData = access.sqlqueryWithParams(compareQuery, new HashMap());
			if (selectData != null && !selectData.isEmpty() && compareData != null && !compareData.isEmpty()) {
				List selectDataList = selectData.stream().filter(selectDataObj -> (selectDataObj != null))
						.map(selectObj -> selectObj[0]).collect(Collectors.toList());
				List compareDataList = compareData.stream().filter(compareDataObj -> (compareDataObj != null))
						.map(compareObj -> compareObj[0]).collect(Collectors.toList());
				List allData = new ArrayList();
				allData.addAll(selectData);
				allData.addAll(compareData);
				JSONObject compareDataObj = new JSONObject();
				for (int i = 0; i < allData.size(); i++) {
					Object[] objData = (Object[]) allData.get(i);
					if (objData != null) {
						if (compareDataObj != null && !compareDataObj.isEmpty()
								&& compareDataObj.get(objData[0]) != null
								&& !((JSONArray) compareDataObj.get(objData[0])).isEmpty()) {
							JSONArray compareDataArr = (JSONArray) compareDataObj.get(objData[0]);
							compareDataArr.remove(2);
							compareDataArr.add(2, objData[2]);
							compareDataObj.put(objData[0], compareDataArr);
						} else {
							JSONArray compareDataArr = new JSONArray();
							compareDataArr.add(objData[0]);
							if (selectDataList != null && !selectDataList.isEmpty() && compareDataList != null
									&& !compareDataList.isEmpty() && selectDataList.contains(objData[0])
									&& !compareDataList.contains(objData[0])) {
								compareDataArr.add(objData[1]);
								compareDataArr.add(0);
							} else if (selectDataList != null && !selectDataList.isEmpty() && compareDataList != null
									&& !compareDataList.isEmpty() && !selectDataList.contains(objData[0])
									&& compareDataList.contains(objData[0])) {
								compareDataArr.add(0);
								compareDataArr.add(objData[2]);
							} else {
								compareDataArr.add(objData[1]);
								compareDataArr.add(objData[2]);
							}
							compareDataObj.put(objData[0], compareDataArr);
						}
					}
				}
				for (Object key : compareDataObj.keySet()) {
					String keyName = (String) key;
					JSONArray allDataArr = (JSONArray) compareDataObj.get(keyName);
					allDataObj.add(allDataArr.toArray());
				}
				chartListObj.put("chartList", allDataObj);
			}
			if (columnKeys != null && !columnKeys.isEmpty()) {
				chartListObj.put("columnKeys", columnKeys);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return chartListObj;
	}

	@Transactional
	public String updteCompareFilterColumn(HttpServletRequest request) {
		String message = "";
		JSONObject compareFilterObj = new JSONObject();
		try {
			String dashbordname = request.getParameter("dashbordName");
			String username = (String) request.getSession(false).getAttribute("ssUsername");
			String roleId = (String) request.getSession(false).getAttribute("ssRole");
			String OrgnId = (String) request.getSession(false).getAttribute("ssOrgId");
			String compareFilterData = request.getParameter("compareFilterData");
			if (compareFilterData != null && !"".equalsIgnoreCase(compareFilterData)
					&& !"null".equalsIgnoreCase(compareFilterData)) {
				compareFilterObj = (JSONObject) JSONValue.parse(compareFilterData);
			}
			if (compareFilterObj != null && !compareFilterObj.isEmpty()) {
				int recordCount = 0;
				String selectQuery = "SELECT * FROM O_RECORD_VISUALIZATION WHERE CHART_TYPE=:CHART_TYPE AND DASHBORD_NAME=:DASHBORD_NAME AND ROLE_ID=:ROLE_ID AND ORGN_ID=:ORGN_ID";
				Map mapData = new HashMap();
				mapData.put("CHART_TYPE", "COMPARE_FILTER");
				mapData.put("DASHBORD_NAME", dashbordname);
				mapData.put("ROLE_ID", roleId);
				mapData.put("ORGN_ID", OrgnId);
				List listData = access.sqlqueryWithParams(selectQuery, mapData);
				if (listData != null && !listData.isEmpty()) {
					recordCount = listData.size();
				}
				if (recordCount == 0) {
					String insertQuery = "INSERT INTO O_RECORD_VISUALIZATION(CHART_TYPE,FILTER_COLUMN,DASHBORD_NAME,ORGN_ID,ROLE_ID,CREATE_BY,EDIT_BY)"
							+ " Values(?,?,?,?,?,?,?)";
					Map<Integer, Object> insertMap = new HashMap<>();
					insertMap.put(1, "COMPARE_FILTER");
					insertMap.put(2, compareFilterObj.toJSONString());
					insertMap.put(3, dashbordname);
					insertMap.put(4, OrgnId);
					insertMap.put(5, roleId);
					insertMap.put(6, username);
					insertMap.put(7, username);
					int count = access.executeNativeUpdateSQLWithSimpleParamsNoAudit(insertQuery, insertMap);
					if (count > 0) {
						message = "Column Inserted Successfully";
					}
				} else {
					String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET FILTER_COLUMN=:FILTER_COLUMN WHERE CHART_TYPE=:CHART_TYPE AND DASHBORD_NAME=:DASHBORD_NAME AND ROLE_ID=:ROLE_ID AND ORGN_ID=:ORGN_ID";
					Map updateMap = new HashMap();
					updateMap.put("FILTER_COLUMN", compareFilterObj.toJSONString());
					updateMap.put("CHART_TYPE", "COMPARE_FILTER");
					updateMap.put("DASHBORD_NAME", dashbordname);
					updateMap.put("ROLE_ID", roleId);
					updateMap.put("ORGN_ID", OrgnId);
					int count = access.executeUpdateSQL(updateQuery, updateMap);
					if (count > 0) {
						message = "Column Updated Successfully";
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	@Transactional
	public JSONObject createFilterHeader(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONObject dataColObj = new JSONObject();
		try {
			String result = "";
			String dashbordname = request.getParameter("dashbordname");
			String filterColumn = request.getParameter("FilterColumn");
			String type = request.getParameter("type");
			String id = request.getParameter("id");
			if (filterColumn != null && !"".equalsIgnoreCase(filterColumn)) {
				List<String> columnList = Arrays.asList(filterColumn.split(","));
				for (int i = 0; i < columnList.size(); i++) {
					String colName = (String) columnList.get(i);
					String colLabel = (colName.split("\\.")[1]).toLowerCase().replace("_", " ");
					colLabel = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
							.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
							.collect(Collectors.joining(" "));
					result += "<div class= 'FilterColumnTitle'>";
					/*
					 * + "<span class='FilterColumnTitleSpanClass col-sm-4'>" + colLabel +
					 * " :</span>";
					 */
//					result += "<span class='surveyFeildsSelection'><div id='FilterColumnId" + colName.split("\\.")[1]
//							+ "Id' class='FilterColumnIdClass' data-tablecolumn='" + colName.split("\\.")[0] + "."
//							+ colName.split("\\.")[1] + "'></span>";
					result += "<span class='surveyFeildsSelection'><div id='FilterColumnId" + colName.replace(".", "_")
							+ "Id' class='FilterColumnIdClass' data-tablecolumn='" + colName.split("\\.")[0] + "."
							+ colName.split("\\.")[1] + "'></span>";
					result += "</div>";
					result += "</div>";
//					dataColObj.put("FilterColumnId" + colName.split("\\.")[1] + "Id",
//							getSurveyAnalyticPartyWiseFilters(colName.split("\\.")[1], colName.split("\\.")[0]));
					dataColObj.put("FilterColumnId" + colName.replace(".", "_") + "Id",
							getSurveyAnalyticPartyWiseFilters(colName.split("\\.")[1], colName.split("\\.")[0]));
				}
				result += "<div class=\"applyBtndiv\">"
						+ "<button class=\"btn btn-primary\" onclick='applyFilterOnGraph();'>Apply</button>"
						+ "<button class=\"btn btn-primary\" onclick='resetHeaderFilters();'>Reset</button>" + "</div>";

			}
			resultObj.put("filterstr", result);
			resultObj.put("dataColObj", dataColObj);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public String getSurveyAnalyticPartyWiseFilters(String tableName, String colName, int count, String chartCount) {
		JSONObject resultObj = new JSONObject();
		try {
			String result = "";
			JSONArray checkBoxDataArr = new JSONArray();
			String operatorId = getOperators(count, chartCount);
			String query = "";
			String colLabel = colName.toLowerCase().replace("_", " ");
			colLabel = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
					.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
					.collect(Collectors.joining(" "));
			if (colName != null && !"".equalsIgnoreCase(colName) && colName.contains("DATE")) {
				result += "<div class='innerFilterDivClass'><div class='visionDashBoardCompareColLabelClass'>"
						+ colLabel + " </div>" + "<div id='tdOperators" + count
						+ "' class='visionDashBoardCompareColOperatorsClass'>" + operatorId + "</div>"
						+ "<div class='visionDashBoardCompareColValuesClass'>" + "<span><input id='tb" + chartCount
						+ count + "' autocomplete='off' value='' "
						+ "class='paramtd_normal jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic' type='text'></span><span><input autocomplete='off' id='tbmin"
						+ chartCount + count + "'"
						+ " style='display:none' value='' class='paramtd_range jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic' type='text'></span>"
						+ "<span style='display:none;' id='to" + chartCount + count
						+ "'>To </span><span><input id='tbmax" + chartCount + count + "' autocomplete='off' value=''"
						+ "style='display:none' class='paramtd_range jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic' type='text'></span>"
						+ "<span><img class='srch_ddw' src='images/range_icon.png' onclick=toggleRange('" + chartCount
						+ count + "')></span></div></div>";

			} else {
				result += "<div class='innerFilterDivClass'><div class='visionDashBoardCompareColLabelClass'>"
						+ colLabel + " </div>" + "<div id='tdOperators" + chartCount + count
						+ "' class='visionDashBoardCompareColOperatorsClass'>" + operatorId + "</div>"
						+ "<div id='tdValues" + chartCount + count + "' class='visionDashBoardCompareColValuesClass'>"
						+ "<div id ='tbValues" + chartCount + count + "'></div></div></div>";
				query = "SELECT DISTINCT " + colName + " FROM " + tableName + " WHERE " + colName
						+ " NOT IN ('Do not want to answer','Phone disconnected','Do not know') AND " + colName
						+ " IS NOT NULL ORDER BY " + colName + " ASC";
				List listData = access.sqlqueryWithParams(query, Collections.EMPTY_MAP);
				if (listData != null && !listData.isEmpty()) {
					for (int i = 0; i < listData.size(); i++) {
						JSONObject checkBoxData = new JSONObject();
						if (listData.get(i) instanceof String) {
							String strData = (String) listData.get(i);
							checkBoxData.put("text", strData);
							checkBoxData.put("value", strData);
						} else {
							checkBoxData.put("text", listData.get(i));
							checkBoxData.put("value", listData.get(i));
						}
						checkBoxDataArr.add(checkBoxData);
					}

				}
			}
			resultObj.put("result", result);
			resultObj.put("checkBoxDataArr", checkBoxDataArr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj.toJSONString();
	}

	@Transactional
	public JSONArray getSurveyAnalyticPartyWiseFilters(String colName, String tableName) {
		JSONArray checkBoxDataArr = new JSONArray();
		Connection connection = null;
		try {
			String whereCond = "";
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT " + colName + " FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					if (columnType != null && !"".equalsIgnoreCase(columnType)
							&& "VARCHAR2".equalsIgnoreCase(columnType)) {
						whereCond = " WHERE " + colName
								+ " NOT IN ('Do not want to answer','Phone disconnected','Do not know')";
					}
				}
			}

			String query = "SELECT DISTINCT " + colName + " FROM " + tableName + whereCond + "  ORDER BY " + colName
					+ " ASC";
			List listData = access.sqlqueryWithParams(query, Collections.EMPTY_MAP);
			if (listData != null && !listData.isEmpty()) {
				for (int i = 0; i < listData.size(); i++) {
					if (i == 0) {
						JSONObject checkBoxData1 = new JSONObject();
						checkBoxData1.put("text", "Select All");
						checkBoxData1.put("value", "Select All");
						checkBoxDataArr.add(checkBoxData1);
					}
					JSONObject checkBoxData = new JSONObject();
					if (listData.get(i) instanceof String) {
						String strData = (String) listData.get(i);
						checkBoxData.put("text", strData);
						checkBoxData.put("value", strData);
					} else {
						checkBoxData.put("text", listData.get(i));
						checkBoxData.put("value", listData.get(i));
					}
					checkBoxDataArr.add(checkBoxData);

				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return checkBoxDataArr;
	}

	public String getOperators(int count, String chartCount) {
		String result = "";
		try {
			JSONObject operators = new JSONObject();
			operators.put("LIKE", "Like");
			operators.put("EQUALS", "=");
			operators.put("NOT EQUALS", "<>");
			operators.put("GREATER THAN", ">");
			operators.put("LESS THAN", "<");
			operators.put("BETWEEN", "Between");
			operators.put("IN", "In");
			operators.put("NOT IN", "Not In");

			result += "<span class='visionDashBoardCompareChartOperatorClass'>";
			result += "<select id='ddw" + chartCount + count + "' class='visionDashBoardCompareChartOperatorClass'>";
			for (Object key : operators.keySet()) {
				String keyName = (String) key;
				String value = (String) operators.get(keyName);
				result += "<option value='" + keyName + "'>" + value + "</option>";
			}
			result += "</select>";
			result += "</span>";
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getChartTable(HttpServletRequest request) {
		String tableName = "";
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String chartType = request.getParameter("type");
			String chartid = request.getParameter("id");

			String selectquery = "SELECT TABLE_NAME FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID AND CHART_TYPE =:CHART_TYPE AND ORGN_ID =:ORGN_ID AND ROLE_ID =:ROLE_ID";
			HashMap datamap = new HashMap();
			datamap.put("CHART_ID", chartid);
			datamap.put("CHART_TYPE", chartType);
			datamap.put("ORGN_ID", (String) request.getSession(false).getAttribute("ssOrgId"));
			datamap.put("ROLE_ID", (String) request.getSession(false).getAttribute("ssRole"));
			List dataList = access.sqlqueryWithParams(selectquery, datamap);
			if (dataList != null && !dataList.isEmpty()) {
				tableName = (String) dataList.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tableName;
	}

	public String getLoadTableColumns(HttpServletRequest request, String tableName) {
		String result = "";
		Connection connection = null;
		try {
			Class.forName(dataBaseDriver);
			String excludeColumn = request.getParameter("excludeColumn");
			if (excludeColumn != null && !"".equalsIgnoreCase(excludeColumn)) {
				excludeColumn = excludeColumn.split("\\.")[1];
			}
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				result = "<div  class='visionVisualizeChartTableToggleClass'>";
				result += "<div class='visionVisualizeChartTableClass'><img src=\"images/nextrightarrow.png\" id=\"VisionImageVisualizationTableId\" class=\"VisionImageVisualizationTableClass\" title=\"Show/Hide Table\"/>"
						+ "<img src=\"images/GridDB.png\" class=\"VisionImageVisualizationTableImageClass\" title=\"Show/Hide Table\"/><h6>"
						+ tableName + "</h6></div>";
				result += "<ul class='visionVisualizationDragColumns'>";
				result += "<div class='columnFilterDiv'><input type='text' id='name' class='columnFilterationClass' placeholder='Search Column'></div>";
				result += "<div class='tableColumnsList'>";
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					String id = tableName + "_" + columnName;
					if (columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)
							&& !columnName.equalsIgnoreCase(excludeColumn)) {
						if (columnType != null && !"".equalsIgnoreCase(columnType)
								&& "NUMBER".equalsIgnoreCase(columnType)) {
							result += "<li id=\"" + id + "\" ondblclick ='dashBoardDrilldownColumns(\"" + id + "\",\""
									+ columnName
									+ "\")'><img src=\"images/sigma-icon.jpg\" class=\"VisionImageVisualizationTableClass\"/>"
									+ columnName + "</li>";
						} else if (columnType != null && !"".equalsIgnoreCase(columnType)
								&& "DATE".equalsIgnoreCase(columnType)) {
							result += "<li id=\"" + id + "\" ondblclick ='dashBoardDrilldownColumns(\"" + id + "\",\""
									+ columnName
									+ "\")'><img src=\"images/calendar.svg\" class=\"VisionImageVisualizationTableClass\"/>"
									+ columnName + "</li>";
						} else {
							result += "<li id=\"" + id + "\" ondblclick ='dashBoardDrilldownColumns(\"" + id + "\",\""
									+ columnName + "\")'>" + columnName + "</li>";
						}
					}
				}
				result += "<li style='display:none'><span>No Columns Found</span></li>";
				result += "</div></ul>";
				result += "</div>";
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	@Transactional
	public JSONObject showDrillDownChart(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			String colName = request.getParameter("colName");
			String tableName = request.getParameter("tableName");
			String paramStr = request.getParameter("paramArray");
			String chartType = request.getParameter("chartType");
			JSONObject chartConfigObj = new JSONObject();
			JSONObject filteredChartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartPropObj");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigObj");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, "",
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			JSONArray paramArray = new JSONArray();
			String filterCond = colName + " IS NOT NULL ";
			if (paramStr != null && !"".equalsIgnoreCase(paramStr)) {
				paramArray = (JSONArray) JSONValue.parse(paramStr);
				for (int i = 0; i < paramArray.size(); i++) {
					JSONObject paramsObj = (JSONObject) paramArray.get(i);
					if (paramsObj != null && !paramsObj.isEmpty()) {
						String whereCond = buildCondition(paramsObj, request);
						if (whereCond != null && !"".equalsIgnoreCase(whereCond)) {
							filterCond = filterCond + " AND " + whereCond;
						}
					}
				}
			}
			List columnKeys = new ArrayList();
			String[] columnNameArr = colName.split("\\.");
			String columnName = columnNameArr[1].replaceAll("\\)", "");
			if (columnName != null && !"".equalsIgnoreCase(columnName) && !"null".equalsIgnoreCase(columnName)) {
				columnName = columnName.replaceAll("_", " ");
			}
			columnKeys.add(columnName);
			columnKeys.add(columnName + "ASCOL0");
			String query = "SELECT " + colName + ",count(" + colName + ") FROM " + tableName + " WHERE " + filterCond
					+ " GROUP BY " + colName + " ";
			Map datamap = new HashMap();
			List selectData = access.sqlqueryWithParamsLimit(query, datamap, 10, 0);
			if (selectData != null && !selectData.isEmpty()) {
				JSONObject dataObj = new JSONObject();
				for (int i = 0; i < selectData.size(); i++) {
					Object[] rowData = (Object[]) selectData.get(i);
					for (int j = 0; j < rowData.length; j++) {
						if (dataObj != null && !dataObj.isEmpty() && dataObj.get(columnKeys.get(j)) != null) {
							JSONArray jsonDataArr = (JSONArray) dataObj.get(columnKeys.get(j));
							if (rowData[j] != null) {
								jsonDataArr.add(rowData[j]);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							}
						} else {
							JSONArray jsonDataArr = new JSONArray();
							if (rowData[j] != null) {
								jsonDataArr.add(rowData[j]);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							}
						}
					}
				}
				chartObj.put("dataObj", dataObj);
				chartObj.put("axisColumnName", columnName);
				chartObj.put("layoutObj", layoutObj);
				chartObj.put("dataPropObj", dataPropObj);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	@Transactional
	public JSONObject fetchBarwithLineEChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			JSONArray axisDataArr = new JSONArray();
			JSONArray valueDataArr = new JSONArray();
			JSONArray seriesDataArr = new JSONArray();
			JSONObject legendObj = new JSONObject();
			String whereCondQuery = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String comboColumns = request.getParameter("comboColumns");
			String filterColumns = request.getParameter("filterColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray comboColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray colsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}
			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (comboColumns != null && !"".equalsIgnoreCase(comboColumns) && !"null".equalsIgnoreCase(comboColumns)) {
				comboColsArr = (JSONArray) JSONValue.parse(comboColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}

			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}
			JSONArray colors = new JSONArray();
			colors.add("#1E90FF");
			colors.add("#FFA500");
			colors.add("#EE6666");
			if (tablesArr != null && !tablesArr.isEmpty()) {
				JSONArray legendData = new JSONArray();
				String tableName = (String) tablesArr.get(0);
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
				}
				String xAxisColumnName = "";
				if (axisColsArr != null && !axisColsArr.isEmpty()) {
					for (int i = 0; i < axisColsArr.size(); i++) {
						JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
						if (axisColObj != null && !axisColObj.isEmpty()) {
							xAxisColumnName = (String) axisColObj.get("columnName");
							String query = "SELECT DISTINCT " + xAxisColumnName + " FROM " + tableName + whereCondQuery;
							List listData = access.sqlqueryWithParams(query, new HashMap());
							JSONArray axisData = new JSONArray();
							JSONObject axisDataObj = new JSONObject();
							if (listData != null && !listData.isEmpty()) {
								for (int j = 0; j < listData.size(); j++) {
									axisData.add(listData.get(j));
								}
							}
							JSONObject alignWIthLabelObj = new JSONObject();
							alignWIthLabelObj.put("alignWithLabel", true);
							axisDataObj.put("type", "category");
							axisDataObj.put("axisTick", alignWIthLabelObj);
							axisDataObj.put("data", axisData);
							axisDataArr.add(axisDataObj);
						}
					}
				}

				if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
					for (int i = 0; i < valuesColsArr.size(); i++) {
						JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
						if (valueColObj != null && !valueColObj.isEmpty()) {
							String columnName = (String) valueColObj.get("columnName");
							String columnLabel = (String) valueColObj.get("columnLabel");
							String aggColumnName = (String) valueColObj.get("aggColumnName");
							String groupBy = "";
							if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)) {
								groupBy = " GROUP BY " + xAxisColumnName;
							}
							String query = "SELECT " + xAxisColumnName + "," + columnName + " FROM " + tableName
									+ whereCondQuery + groupBy;
							List listData = access.sqlqueryWithParams(query, new HashMap());
							JSONArray valuesData = new JSONArray();
							JSONObject valuesObj = new JSONObject();
							if (listData != null && !listData.isEmpty()) {
								for (int j = 0; j < listData.size(); j++) {
									Object[] objData = (Object[]) listData.get(j);
									valuesData.add(objData[1]);
								}
							}
							legendData.add(columnLabel);
							JSONObject lineStyleObj = new JSONObject();
							lineStyleObj.put("color", "#91CC75");
							JSONObject axisLabelObj = new JSONObject();
							axisLabelObj.put("formatter", "{value}");
							JSONObject axisLineObj = new JSONObject();
							axisLineObj.put("show", true);
							axisLineObj.put("lineStyle", lineStyleObj);
							valuesObj.put("type", "value");
							valuesObj.put("name", columnLabel);
							valuesObj.put("position", "left");
							valuesObj.put("alignTicks", true);
							valuesObj.put("offset", 30);
							valuesObj.put("axisLine", axisLineObj);
							valuesObj.put("axisLabel", axisLabelObj);
							valueDataArr.add(valuesObj);

							JSONObject seriesObj = new JSONObject();
							seriesObj.put("name", columnLabel);
							seriesObj.put("type", "bar");
							seriesObj.put("data", valuesData);
							seriesDataArr.add(seriesObj);
						}
					}
				}

				if (comboColsArr != null && !comboColsArr.isEmpty()) {
					for (int i = 0; i < comboColsArr.size(); i++) {
						JSONObject comboColObj = (JSONObject) comboColsArr.get(i);
						if (comboColObj != null && !comboColObj.isEmpty()) {
							String columnName = (String) comboColObj.get("columnName");
							String columnLabel = (String) comboColObj.get("columnLabel");
							String aggColumnName = (String) comboColObj.get("aggColumnName");
							String groupBy = "";
							if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)) {
								groupBy = " GROUP BY " + xAxisColumnName;
							}
							String query = "SELECT " + xAxisColumnName + "," + columnName + " FROM " + tableName
									+ whereCondQuery + groupBy;
							List listData = access.sqlqueryWithParams(query, new HashMap());
							JSONArray valuesData = new JSONArray();
							JSONObject valuesObj = new JSONObject();
							if (listData != null && !listData.isEmpty()) {
								for (int j = 0; j < listData.size(); j++) {
									Object[] objData = (Object[]) listData.get(j);
									valuesData.add(objData[1]);
								}
							}
							legendData.add(columnLabel);
							JSONObject lineStyleObj = new JSONObject();
							lineStyleObj.put("color", "#EE6666");
							JSONObject axisLabelObj = new JSONObject();
							axisLabelObj.put("formatter", "{value}");
							JSONObject axisLineObj = new JSONObject();
							axisLineObj.put("show", true);
							axisLineObj.put("lineStyle", lineStyleObj);
							valuesObj.put("type", "value");
							valuesObj.put("name", columnLabel);
							valuesObj.put("position", "right");
							valuesObj.put("offset", 30);
							valuesObj.put("alignTicks", true);
							valuesObj.put("axisLine", axisLineObj);
							valuesObj.put("axisLabel", axisLabelObj);
							valueDataArr.add(valuesObj);

							JSONObject seriesObj = new JSONObject();
							seriesObj.put("name", columnLabel);
							seriesObj.put("type", "line");
							seriesObj.put("data", valuesData);
							seriesObj.put("yAxisIndex", 1);
							seriesDataArr.add(seriesObj);
						}
					}
				}
				legendObj.put("data", legendData);
			}
			chartObj.put("legend", legendObj);
			chartObj.put("xAxis", axisDataArr);
			chartObj.put("yAxis", valueDataArr);
			chartObj.put("series", seriesDataArr);
			chartObj.put("color", colors);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	@Transactional
	public JSONObject fetchTreeMapEChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			JSONArray dataArr = new JSONArray();
			Double currencyConversionRate = null;
			String whereCondQuery = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String aggregateColumns = request.getParameter("aggregateColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String radioButtons = request.getParameter("radioButtons");
			String columnsNameForComplexQuery = request.getParameter("columnsListForComplexQuery");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray colsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}
			if (radioButtons != null && !"".equalsIgnoreCase(radioButtons)) {
				chartObj.put("radioButtonStr", getradioButtonsStr(chartId, radioButtons));
			}
			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (aggregateColumns != null && !"".equalsIgnoreCase(aggregateColumns)
					&& !"null".equalsIgnoreCase(aggregateColumns)) {
				aggregateColsArr = (JSONArray) JSONValue.parse(aggregateColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {
							aggregateColsArr.add(aggColumnName);
						}
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}
			String currencyConversionEvent = request.getParameter("isCurrencyConversionEvent");
			boolean isCurrencyConversionEvent = false;
			if (currencyConversionEvent != null && !"".equalsIgnoreCase(currencyConversionEvent)
					&& !"null".equalsIgnoreCase(currencyConversionEvent)) {
				isCurrencyConversionEvent = Boolean.parseBoolean(currencyConversionEvent);
				currencyConversionRate = getCurrencyConversionRate(request);
			}
			
			if (colsArr != null && !colsArr.isEmpty() && colsArr.size() >= 2 && tablesArr != null
					&& !tablesArr.isEmpty()) {
				int colsArrLength = colsArr.size();
				String tableName = (String) tablesArr.get(0);
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
				}
				String groupBy = "";
				if (colsArr != null && !colsArr.isEmpty() && colsArr.size() == 2) {
					String xAxisColumn = (String) colsArr.get(0);
					String dataColumn = (String) colsArr.get(1);
					if (!(aggregateColsArr != null && !aggregateColsArr.isEmpty())) {
						dataColumn = "SUM(" + dataColumn + ")";
					}
					groupBy = " GROUP BY " + xAxisColumn;
					String xAxisQuery = "SELECT DISTINCT " + xAxisColumn + "," + dataColumn + " FROM " + tableName + " "
							+ whereCondQuery + groupBy + " ORDER BY " + xAxisColumn + " ASC";
					List selectData = access.sqlqueryWithParams(xAxisQuery, new HashMap());
					if (selectData != null && !selectData.isEmpty()) {
						for (int j = 0; j < selectData.size(); j++) {
							Object[] objData = (Object[]) selectData.get(j);
							if (objData != null) {
								JSONObject dataObj = new JSONObject();
								dataObj.put("name", objData[0]);
								if (objData[1] != null && currencyConversionRate != null && isCurrencyConversionEvent) {
									double chartValueIndouble = (Double) dashboardutils
											.getRequiredObjectTypeFromObject(objData[1], "BigDecimal", "Double");
									double convertedCurrencyValue = chartValueIndouble * currencyConversionRate;
									dataObj.put("value", convertedCurrencyValue);
								} else {
									dataObj.put("value", objData[1]);
								}
								dataArr.add(dataObj);
							}
						}
					}
				} else if (colsArr != null && !colsArr.isEmpty() && colsArr.size() >= 3) {
					JSONArray xAxisColumnArr1 = new JSONArray();
					for (int y = 0; y < colsArrLength - 2; y++) {
						xAxisColumnArr1.add(colsArr.get(y));
					}
					JSONArray yAxisColumnArr1 = new JSONArray();          
					for (int y = 1; y < colsArrLength - 1; y++) {
						yAxisColumnArr1.add(colsArr.get(y));
					}
					String dataColumn = (String) colsArr.get(colsArrLength - 1);
					String nameQuery = "";
					String parentName = "";
					for (int x = 0; x < yAxisColumnArr1.size(); x++) {
						if (x > 0) {
							nameQuery += "," + xAxisColumnArr1.get(x - 1);
						}
						String xAxisQuery = "SELECT DISTINCT " + xAxisColumnArr1.get(x) + " FROM " + tableName + " "
								+ whereCondQuery // xAxisColumn
								+ " ORDER BY " + xAxisColumnArr1.get(x) + " ASC"; //
						List selectData = access.sqlqueryWithParams(xAxisQuery, new HashMap());
						if (selectData != null && !selectData.isEmpty()) {
							for (int j = 0; j < selectData.size(); j++) {//
								groupBy = " GROUP BY " + yAxisColumnArr1.get(x); // groupBy = " GROUP BY " +
																					// yAxisColumn;
																					// yAxisColumnArr1.get(x)
								String xAxisVal = (String) selectData.get(j);
								String xAxisCondQuery = "";
								String yAxisCondQuery = yAxisColumnArr1.get(x) + " IS NOT NULL"; // String
																									// yAxisCondQuery =
																									// yAxisColumn + "
																									// IS NOT NULL";
								if (!(whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
										&& !"null".equalsIgnoreCase(whereCondQuery))) {
									xAxisCondQuery += " WHERE " + xAxisColumnArr1.get(x) + " = '" + xAxisVal + "'"; //
									xAxisCondQuery += " AND " + yAxisCondQuery;
								} else {
									xAxisCondQuery = " AND " + xAxisColumnArr1.get(x) + " = '" + xAxisVal + "'"; //
									xAxisCondQuery += " AND " + yAxisCondQuery;
								}
                                String childQuery = ""; 
								if(dataColumn.contains("(")) {
									childQuery = "SELECT DISTINCT " + yAxisColumnArr1.get(x)  + "," + dataColumn
									+ nameQuery + " FROM " + tableName + " " + whereCondQuery + xAxisCondQuery
									+ groupBy + nameQuery;
								}
								else {
									childQuery = "SELECT DISTINCT " + yAxisColumnArr1.get(x) + "," + dataColumn
											+ nameQuery + " FROM " + tableName + " " + whereCondQuery + xAxisCondQuery
											+ groupBy + "," + dataColumn + nameQuery;
								}
								List childData = access.sqlqueryWithParams(childQuery, new HashMap());
								JSONArray childArr = new JSONArray();
								long longVal = 0;
								Object[] parentArray = null;
								for (int k = 0; k < childData.size(); k++) {
									Object[] objData = (Object[]) childData.get(k);
									if (objData != null) {
										JSONObject childObj = new JSONObject();
										childObj.put("name", objData[0]);
										if (objData.length >= 3) {
											parentName = (String) objData[2];
											parentArray = Arrays.copyOfRange(objData, 2, objData.length);
											System.out.println(Arrays.toString(parentArray));
										}
										if (objData[1] != null && currencyConversionRate != null
												&& isCurrencyConversionEvent) {
											double chartValueIndouble = (Double) dashboardutils
													.getRequiredObjectTypeFromObject(objData[1], "BigDecimal",
															"Double");
											double convertedCurrencyValue = chartValueIndouble * currencyConversionRate;
											childObj.put("value", convertedCurrencyValue);
											longVal += (long) convertedCurrencyValue;
										} else {
											childObj.put("value", objData[1]);
											longVal += (objData[1] != null ? ((BigDecimal) objData[1]).longValue() : 0);
										}
										childArr.add(childObj);
										if (x > 0) {
											dataArr = dashboardutils.insertChildObj(dataArr, childObj, x, xAxisVal,
													parentName, childArr);
										}
									}
								}
								if (x == 0) {
									JSONObject dataObj = new JSONObject();
									dataObj.put("name", xAxisVal);
									dataObj.put("value", longVal);
									dataObj.put("children", childArr);
									dataArr.add(dataObj);
								}

							} //

						}
					} // FOR LOOP CLOSING STATEMENT

				} // ELSE IF CLOSING STATEMENT

				chartObj.put("data", dataArr);
				chartObj.put("tableName", tableName);
				chartObj.put("dataPropObject", dataPropObj);
				chartObj.put("layout", layoutObj);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	@Transactional
	public JSONObject getChartColumnsForm(HttpServletRequest request) {
		JSONObject chartSlicerData = new JSONObject();
		Connection connection = null;
		try {
			String result = "";
			String columnStr = "";
			String chartId = request.getParameter("chartId");
			String resultStr = "<div id='HomeSlicerColumndataId' class = 'HomeSlicerColumndataClass'>"
					+ "<div id=\"VisualizeBIHomeSlicerColumns\"></div>"
					+ "<div id=\"visualizeChartHomeSlicerData\" class=\"visualizeChartHomeSlicerClass\"></div>"
					+ "</div>";
			String tableQuery = "SELECT DISTINCT TABLE_NAME FROM O_RECORD_VISUALIZATION WHERE ROLE_ID =:ROLE_ID AND ORGN_ID=:ORGN_ID AND CHART_TYPE NOT IN('CARD','FILTER','COMPARE_FILTER') AND CHART_ID =:CHART_ID";
			Map tableMap = new HashMap();
			tableMap.put("CHART_ID", chartId);
			tableMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
			tableMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
			List listData = access.sqlqueryWithParams(tableQuery, tableMap);
			if (listData != null && !listData.isEmpty()) {
				for (int j = 0; j < listData.size(); j++) {
					String tableName = (String) listData.get(j);
					Class.forName(dataBaseDriver);
					connection = DriverManager.getConnection(dbURL, userName, password);
					Statement statement = connection.createStatement();
					ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
					ResultSetMetaData metadata = results.getMetaData();
					int columnCount = metadata.getColumnCount();
					if (columnCount > 0) {
						result += "<div  class='visionVisualizeHomeChartTableToggleClass'>";
						result += "<div class='visionVisualizeChartTableClass'><img src=\"images/nextrightarrow.png\" id=\"VisionImageVisualizationTableId\" class=\"VisionImageVisualizationHomeTableClass\" title=\"Show/Hide Table\"/>"
								+ "<img src=\"images/GridDB.png\" class=\"VisionImageVisualizationTableImageClass\" title=\"Show/Hide Table\"/><h6>"
								+ tableName + "</h6></div>";
						result += "<ul class='visionVisualizationDragColumns'>";
						result += "<div class='columnFilterDiv'><input type='text' id='name' class='columnFilterationClass' placeholder='Search Column'/></div>";
						result += "<div class='homechartSlicerColumnsDiv'>";
						for (int i = 1; i <= columnCount; i++) {
							String columnName = metadata.getColumnName(i);
							String columnType = metadata.getColumnTypeName(i);
							String id = tableName + "_" + columnName;
							if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "NUMBER".equalsIgnoreCase(columnType)) {
								result += "<li id=\"" + id
										+ "liId\"><img src=\"images/sigma-icon.jpg\" class=\"VisionImageVisualizationHomeTableClass\"/><span class='columnNameIS'>"
										+ columnName + "</span>"
										+ "<span class='columnAddImg'><img src='images/image2vector.svg' class='addcolumnIcon' onclick=\"dashBoardHeaderFilter('"
										+ id + "','" + columnName + "')\"/></span>" + "</li>";
							} else if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "DATE".equalsIgnoreCase(columnType)) {
								result += "<li id=\"" + id
										+ "liId\"><img src=\"images/calendar.svg\" class=\"VisionImageVisualizationHomeTableClass\"/><span class='columnNameIS'>"
										+ columnName + "</span>"
										+ "<span class='columnAddImg'><img src='images/image2vector.svg' class='addcolumnIcon' onclick=\"dashBoardHeaderFilter('"
										+ id + "','" + columnName + "')\"/></span>" + "</li>";
							} else {
								result += "<li id=\"" + id + "liId\"><span class='columnNameIS'>" + columnName
										+ "</span>"
										+ "<span class='columnAddImg'><img src='images/image2vector.svg' class='addcolumnIcon' onclick=\"dashBoardHeaderFilter('"
										+ id + "','" + columnName + "')\"/></span>" + "</li>";
							}
						}
						result += "<li style='display:none'><span>No Columns Found</span></li>";
						result += "</div></ul>";
						result += "</div>";
					}

				}
				columnStr += "<ul class='visionVisualizationDragColumns1'>";
				columnStr += "<div class='homefilterColumnsDiv'>";
				String filtercolQUery = "SELECT VISUALIZE_CUST_COL7 FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID AND ROLE_ID =:ROLE_ID AND ORGN_ID=:ORGN_ID";
				Map filterColMap = new HashMap();
				filterColMap.put("CHART_ID", chartId);
				filterColMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
				filterColMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
				List filterColumn = access.sqlqueryWithParams(filtercolQUery, filterColMap);
				if (filterColumn != null && !filterColumn.isEmpty()) {
					for (int i = 0; i < filterColumn.size(); i++) {
						String filteredColumn = (String) filterColumn.get(i);
						if (filteredColumn != null && !"".equalsIgnoreCase(filteredColumn)) {
							String[] columnArr = filteredColumn.split("\\,");
							for (int j = 0; j < columnArr.length; j++) {
								String tableAttr = columnArr[j];
								String tableArr[] = tableAttr.split("\\.");
								String id = tableArr[0] + "_" + tableArr[1];
								columnStr += "<li id=\"" + id + "SavedliId\">" + tableArr[1]
										+ "<img src='images/close_white.png'  class='VisualizeColumnCancelClass' onclick=deleteHeaderFilterColumns(\""
										+ id + "SavedliId\")  title='Close Chart'/></li>";
							}
						}
					}
				}
				columnStr += "</div>" + "</ul>";
				chartSlicerData.put("columnStr", columnStr);
				chartSlicerData.put("result", result);
				chartSlicerData.put("resultStr", resultStr);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartSlicerData;
	}

	public String getradioButtonsStr(String chartId, String radioButtons) {
		String radioButtonsStr = "";
		try {
			if (radioButtons != null && !"".equalsIgnoreCase(radioButtons)) {
				String radioButtonArr[] = radioButtons.split(",");
				if (radioButtonArr != null) {
					for (int i = 0; i < radioButtonArr.length; i++) {
						String radioStr = radioButtonArr[i];
						String radioLabel = (radioStr.split("\\.")[1]).toLowerCase().replace("_", " ");
						String radioStrLabel = Stream.of(radioLabel.trim().split("\\s"))
								.filter(word -> word.length() > 0)
								.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
								.collect(Collectors.joining(" "));
						radioButtonsStr += "<span class='radioCheckinput'><input type=\"radio\" id=\"radioStr\" class=\"fav_language\" name=\"fav_language\" value='"
								+ radioStr + "'></span><span class='radioText'>" + radioStrLabel + "</span>";
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return radioButtonsStr;
	}

	@Transactional
	public JSONObject getchartPropertiesobj(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String chartType = request.getParameter("type");
			String chartid = request.getParameter("id");

			String selectquery = "SELECT X_AXIS_VALUE, Y_AXIS_VALUE,TABLE_NAME,AGGRIGATE_COLUMNS,FILTER_CONDITION,CHART_PROPERTIES,CHART_CONFIG_OBJECT,VISUALIZE_CUST_COL8,CHART_TYPE FROM O_RECORD_VISUALIZATION WHERE  CHART_ID =:CHART_ID";
			HashMap datamap = new HashMap();
//            datamap.put("CREATE_BY", userName);
			datamap.put("CHART_ID", chartid);
			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					JSONObject dataobj = new JSONObject();
					dataobj.put("xAxix", rowData[0]);
					dataobj.put("yAxix", rowData[1]);
					dataobj.put("table", rowData[2]);
					dataobj.put("aggregateType", rowData[3]);
					dataobj.put("filterColumns", rowData[4]);
					dataobj.put("chartConfigObj", rowData[5]);
					dataobj.put("chartPropObj", rowData[6]);
					dataobj.put("colorsObj", rowData[7]);
					dataobj.put("type", rowData[8]);
					dataarr.add(dataobj);
				}
				tabledataobj.put("dataarr", dataarr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public JSONObject updateGraphProperties(HttpServletRequest request) {
		String result = "";
		JSONObject resultObj = new JSONObject();
		try {

			String chartPropObject = request.getParameter("chartConfigObjchartConfigObj");
			String chartCOnfigobj = request.getParameter("chartPropObj");
			String chartId = request.getParameter("chartid");
			String charttype = request.getParameter("charttype");
			String updateQuery = "update O_RECORD_VISUALIZATION set CHART_PROPERTIES =:CHART_PROPERTIES,CHART_CONFIG_OBJECT =:CHART_CONFIG_OBJECT,CHART_TYPE =:CHART_TYPE where CHART_ID =:CHART_ID";
			HashMap updatemap = new HashMap();
			updatemap.put("CHART_PROPERTIES", chartPropObject);
			updatemap.put("CHART_CONFIG_OBJECT", chartCOnfigobj);
			updatemap.put("CHART_TYPE", charttype);
			updatemap.put("CHART_ID", chartId);
//            updatemap.put("CREATE_BY", (String) request.getSession(false).getAttribute("ssUsername"));
			int count = access.executeUpdateSQLNoAudit(updateQuery, updatemap);
			if (count > 0) {
				result += "<div id ='dashboardGraphId' class = 'dashboardGraphClass'><img src='images/successTick.gif' width='50%' /><p>DashBoard Updated Successfully</p></div>";
			} else {
				result += "<div id ='dashboardGraphId' class = 'dashboardGraphClass'><img src='images/failedTick.gif' width='50%' /><p>Faild To Update</p></div>";
			}
			System.out.println("update succesfull.");
			resultObj.put("result", result);
		} catch (Exception e) {

		}
		return resultObj;
	}

	@Transactional
	public JSONObject getTreeMapExchangeLevels(HttpServletRequest request) {
		String result = "";
		JSONObject resultObj = new JSONObject();
		try {
			String chartId = request.getParameter("chartId");
			String charttype = request.getParameter("charttype");
			String updateQuery = "SELECT X_AXIS_VALUE  FROM O_RECORD_VISUALIZATION WHERE ORGN_ID =:ORGN_ID AND ROLE_ID =:ROLE_ID AND CHART_ID =:CHART_ID";
			HashMap updatemap = new HashMap();
			updatemap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
			updatemap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
			updatemap.put("CHART_ID", chartId);
			List exchangeList = access.sqlqueryWithParams(updateQuery, updatemap);
			if (exchangeList != null && !exchangeList.isEmpty()) {
				String exchangeStr = (String) exchangeList.get(0);
				if (exchangeStr != null && !"".equalsIgnoreCase(exchangeStr)) {
					JSONArray exchangeArr = (JSONArray) JSONValue.parse(exchangeStr);
					if (exchangeArr != null && !exchangeArr.isEmpty()) {
						for (int i = 0; i < exchangeArr.size(); i++) {
							JSONObject exchangeObj = (JSONObject) exchangeArr.get(i);
							if (exchangeObj != null && !exchangeObj.isEmpty()) {
								String tableName = (String) exchangeObj.get("tableName");
								String columnName = (String) exchangeObj.get("columnName");
								result += "<li id='" + columnName
										+ "' class='treeMapExchangeLevelsClassSort' data-tableName='" + tableName
										+ "' data-columnName='" + columnName + "'>" + columnName + "</li>";
							}
						}
					}
				}
			}
			resultObj.put("result", result);
		} catch (Exception e) {

		}
		return resultObj;
	}

	@Transactional
	public JSONObject getExchaneLevelsData(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String chartIds = request.getParameter("chartIds");
			String updateflag = request.getParameter("flag");
			if (updateflag != null && "Y".equalsIgnoreCase(updateflag)) {
				updateExchangeLevelsdata(request);
			}
			HashMap datamap = new HashMap();
			String selectquery = "SELECT " + "X_AXIS_VALUE, "// 0
					+ "Y_AXIS_VALUE,"// 1
					+ "CHART_TYPE,"// 2
					+ "TABLE_NAME,"// 3
					+ "CHART_ID,"// 4
					+ "AGGRIGATE_COLUMNS, "// 5
					+ "FILTER_CONDITION, "// 6
					+ "CHART_PROPERTIES, "// 7
					+ "CHART_CONFIG_OBJECT, "// 8
					+ "VISUALIZE_CUST_COL10, "// 9
					+ "CHART_TITTLE, " // 10
					+ "VISUALIZE_CUST_COL8, " // 11
					+ "VISUALIZE_CUST_COL9, " // 12
					+ "VISUALIZE_CUST_COL5, " // 13
					+ "FILTER_COLUMN, " // 14
					+ "VISUALIZE_CUST_COL6, " // 15
					+ "VISUALIZE_CUST_COL7, " // 16
					+ "COMBO_VALUE " // 17
					+ "FROM " + "O_RECORD_VISUALIZATION " + "WHERE ROLE_ID =:ROLE_ID ";
			if (chartIds != null && !"".equalsIgnoreCase(chartIds) && !"null".equalsIgnoreCase(chartIds)) {
				JSONArray chartsArr = (JSONArray) JSONValue.parse(chartIds);
				List chartList = new ArrayList(chartsArr);
				chartIds = (String) chartList.stream().collect(Collectors.joining("','", "'", "'"));
				selectquery += " AND CHART_ID IN(" + chartIds + ")";
			}
			selectquery += "ORDER BY CHART_SEQUENCE_NO";
			datamap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));

			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					JSONObject dataobj = new JSONObject();
					dataobj.put("xAxix", rowData[0]);
					dataobj.put("yAxix", rowData[1]);
					dataobj.put("type", rowData[2]);
					dataobj.put("table", rowData[3]);
					dataobj.put("chartid", rowData[4]);
					dataobj.put("aggColumnName", rowData[5]);
					dataobj.put("filterCondition", rowData[6]);
					dataobj.put("chartPropObj", rowData[7]);
					dataobj.put("chartConfigObj", rowData[8]);
					dataobj.put("labelLegend", rowData[9]);
					dataobj.put("Lebel", rowData[10]);
					dataobj.put("colorsObj", rowData[11]);
					dataobj.put("chartConfigToggleStatus", rowData[12]);
					dataobj.put("compareChartsFlag", rowData[13]);
					dataobj.put("homeFilterColumn", rowData[14]);
					dataobj.put("fetchQuery", rowData[15]);
					dataobj.put("radioButtons", rowData[16]);
					dataobj.put("comboValue", rowData[17]);
					dataarr.add(dataobj);
				}
				tabledataobj.put("dataarr", dataarr);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public String updateExchangeLevelsdata(HttpServletRequest request) {
		String Result = "";
		int updatecount = 0;
		try {
			String itemsArr = request.getParameter("itemsArr");
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String UpdateQuery = "update O_RECORD_VISUALIZATION set X_AXIS_VALUE =:X_AXIS_VALUE  WHERE CHART_ID =:CHART_ID";
			Map<String, Object> updateMap = new HashMap<>();
			updateMap.put("X_AXIS_VALUE", itemsArr); // WHERE_CON
			updateMap.put("CHART_ID", chartId); // chartId
			System.out.println("updateMap:::" + updateMap);
			updatecount = access.executeUpdateSQLNoAudit(UpdateQuery, updateMap);
			if (updatecount != 0) {
				Result = "Updated successFully";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;

	}

	@Transactional
	public JSONObject createTableasFile(HttpServletRequest request, HttpServletResponse response) {
		Connection connection = null;
		JSONObject resultObj = new JSONObject();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			String batchNumber = "";
			String user = (String) request.getSession(false).getAttribute("ssUsername");
			String ssOrgName = (String) request.getSession(false).getAttribute("ssOrgName");
			String stgTable = request.getParameter("tableName");
			stgTable = stgTable.replaceAll("[^a-zA-Z0-9_]", "_");
			String fileName = request.getParameter("fileName");
			String filePath1 = request.getParameter("filePath");
//			String filePath = "C:/Files/TreeDMImport" + File.separator + userName;
			String filePath = fileStoreHomedirectory + "TreeDMImport/" + user + "/" + filePath1;
//			String filePath = fileStoreHomedirectory+"\\TreeDMImport\\" + user + "\\" + filePath1;
			String fileExtension = FilenameUtils.getExtension(filePath1);
			List<String> toColumnsDataTypes = null;
			List<String> dataList = null;
			List<String> dataTypesList = null;
			List<String> headersList = dashboardutils.getHeadersOfImportedFile(request, response, filePath);
			List<String> headerTypeList = getColumnTypesOfImportedFile(request, response, filePath);
			if (!dashboardutils.isNullOrEmpty(fileExtension)) {
				if (fileExtension.equalsIgnoreCase("xls") || fileExtension.equalsIgnoreCase("xlsx")) {
					toColumnsDataTypes = headerTypeList;
					dataList = dashboardutils.readExcelFile(request, filePath, fileName);
					dataTypesList = dashboardutils.getHeaderDataTypesOfImportedFile(request, filePath);
				} else if (fileExtension.equalsIgnoreCase("csv") || fileExtension.equalsIgnoreCase("txt")) {
					toColumnsDataTypes = headerTypeList.stream().map((e) -> e.split("\\(")[0])
							.collect(Collectors.toList());
					dataList = dashboardutils.readCSV(request, response, filePath, toColumnsDataTypes);
					headersList = headersList.stream().map(e -> e.toUpperCase()).collect(Collectors.toList());
					dataTypesList = headerTypeList;
				}
			}
//			List<String> headersList = dashboardutils.getHeadersOfImportedFile(request, response, filePath);
//			List headerTypeList = getColumnTypesOfImportedFile(request, response, filePath);
//			List toColumnsDataTypes = headerTypeList;
//            if (headerTypeList != null && !headerTypeList.isEmpty()) {
//                for (int i = 0; i < headerTypeList.size(); i++) {
//                    String headerName = (String) headerTypeList.get(i);
//                    toColumnsDataTypes.add(headerName);
//                }
//            }
//			List dataList = dashboardutils.readExcelFile(request, filePath, fileName);
			JSONObject dbConnObj = new PilogUtilities().getDatabaseDetails(dataBaseDriver, dbURL, userName, password,
					"Current_V10");
			String insertQuery = "";
			connection = dashboardutils.getCurrentConnection();
			Map<Integer, Object> insertMap = new HashMap<>();
			List<Object[]> newDataList = new ArrayList();
//			List dataTypesList = dashboardutils.getHeaderDataTypesOfImportedFile(request, filePath);
			List fromColumnsList = dashboardutils.fileHeaderValidations(headersList);
			try {
				String deletequery = "DROP TABLE " + stgTable;
				preparedStatement = connection.prepareStatement(deletequery);
				preparedStatement.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}

			boolean auditFlag = false;
            if(fromColumnsList !=null && !fromColumnsList.isEmpty() && !fromColumnsList.contains("AUDIT_ID"))
            {           
			fromColumnsList.add("AUDIT_ID");
			dataTypesList.add(
					"VARCHAR2(100 CHAR)               DEFAULT '" + stgTable.toUpperCase() + "'||SYS_GUID() NOT NULL");
			auditFlag = true;
            }

			String createTableQuery = "CREATE TABLE " + stgTable + "( ";
			for (int i = 0; i < fromColumnsList.size(); i++) {
				if (i < (fromColumnsList.size() - 1)) {
					createTableQuery += fromColumnsList.get(i) + " " + dataTypesList.get(i) + " , ";
				} else {
					createTableQuery += fromColumnsList.get(i) + " " + dataTypesList.get(i) + ")";
				}
			}
			preparedStatement = connection.prepareStatement(createTableQuery);
			preparedStatement.execute();
			if(auditFlag) {
			fromColumnsList.remove("AUDIT_ID");
			}
			String columnsStr = (String) fromColumnsList.stream().map(e -> e).collect(Collectors.joining(","));
			String paramsStr = (String) fromColumnsList.stream().map(e -> "?").collect(Collectors.joining(","));
			insertQuery = "INSERT INTO " + stgTable + " (" + columnsStr + " ) VALUES ( " + paramsStr + ")";

			preparedStatement = connection.prepareStatement(insertQuery);
			int insertCount = insertDataIntoTable(request, stgTable, preparedStatement, headersList, dataList,
					headersList, toColumnsDataTypes, "ORACLE", null);
			String message = insertCount + " Records Imported with Batch no " + batchNumber;
			resultObj.put("message", message);
			resultObj.put("tableName", stgTable);

			String insertTbaleQuery = "INSERT INTO C_ETL_DAL_AUTHORIZATION(TABLE_NAME,CREATE_BY) VALUES('" + stgTable
					+ "','" + user + "')";
			try {
				int cnt = access.executeUpdateSQL(insertTbaleQuery);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;

	}

	@Transactional
	public int insertDataIntoTable(HttpServletRequest request, String tableName, PreparedStatement preparedStatement,
			List columnsList, List totalData, List toColumnsList, List toColumnsDataTypes, String fromDBType,
			String jobId) {

		int count = 0;
		int insertedCount = 0;
		try {

			int batchSize = 10000;
			if (totalData.size() > batchSize) {
				while (insertedCount < totalData.size()) {
//                    System.err.println("insertedCount::: "+insertedCount);
//                    System.err.println("batchSize::: "+batchSize);
					if ((insertedCount + batchSize) > totalData.size()) {
						batchSize = (totalData.size() - insertedCount);
					}
					for (int i = insertedCount; i < (insertedCount + batchSize); i++) {
						Object[] rowData = (Object[]) totalData.get(i);
						for (int j = 0; j < columnsList.size(); j++) {

							Object value = convertIntoDBValue(rowData[j], (String) toColumnsDataTypes.get(j),
									fromDBType);
//                            String dataType = String.valueOf(toColumnsDataTypes.get(j));
//                            if (dataType.equalsIgnoreCase("NUMBER")) {
//                                Object value1 = value;
//                            }
							preparedStatement.setObject(j + 1, value);
						}
						preparedStatement.addBatch();
					}
					int[] countarray = preparedStatement.executeBatch();
					count = countarray.length;
					insertedCount += batchSize;
				}

			} else {
				for (int i = 0; i < totalData.size(); i++) {
					Object[] rowData = (Object[]) totalData.get(i);
					for (int j = 0; j < columnsList.size(); j++) {
						Object value = convertIntoDBValue(rowData[j], (String) toColumnsDataTypes.get(j), fromDBType);
						preparedStatement.setObject(j + 1, value);
					}

					preparedStatement.addBatch();
				}
				int[] countarray = preparedStatement.executeBatch();
				insertedCount = countarray.length;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return insertedCount;
	}

	public Object convertIntoDBValue(Object value, String columnType, String fromDBType) {
		try {
			if (value instanceof String) {
				value = String.valueOf(value).trim();
			}
			if (value != null && columnType != null && !"".equalsIgnoreCase(columnType)
					&& !"null".equalsIgnoreCase(columnType)) {

				if (value instanceof byte[]) {
//                    value = new BASE64Encoder().encode((byte[])value);
//                 value = new String((byte[])value, StandardCharsets.UTF_16BE);
//                    String abc = HexFormat.of().formatHex(value);
					String hash = "";

					for (byte aux : (byte[]) value) {
						int b = aux & 0xff;
						if (Integer.toHexString(b).length() == 1) {
							hash += "0";
						}
						hash += Integer.toHexString(b);
					}
					value = hash.toUpperCase();
				}
				if ("DATE".equalsIgnoreCase(columnType) || "TIMESTAMP".equalsIgnoreCase(columnType)
						|| "DATETIME".equalsIgnoreCase(columnType)) {
					try {
						if (fromDBType != null && fromDBType == "SAP") {
							SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");
							try {
								if (String.valueOf(value).equals("00000000")) {
									Date localDate = simpleDateFormat.parse(simpleDateFormat.format(new Date()));
									if (localDate != null) {
										java.sql.Timestamp sqlDate = new java.sql.Timestamp(localDate.getTime());
										value = sqlDate;
									}
								} else {
									Date localDate = new SimpleDateFormat("yyyyMMdd").parse(String.valueOf(value));
									if (localDate != null) {
										java.sql.Timestamp sqlDate = new java.sql.Timestamp(localDate.getTime());
										value = sqlDate;
									}
								}
							} catch (Exception e) {
								Date localDate = simpleDateFormat.parse(simpleDateFormat.format(new Date()));
								if (localDate != null) {
									java.sql.Timestamp sqlDate = new java.sql.Timestamp(localDate.getTime());
									value = sqlDate;
								}
							}

						} else {

							SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.sss");

							try {
								Date localDate = null;
								if (value instanceof String) {
									localDate = simpleDateFormat.parse(String.valueOf(value));
								} else {
									localDate = simpleDateFormat.parse(simpleDateFormat.format(value));
								}

								if (localDate != null) {
									java.sql.Timestamp sqlDate = new java.sql.Timestamp(localDate.getTime());
									value = sqlDate;
								}
							} catch (Exception e) {
								simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
								try {
									Date localDate = null;
									if (value instanceof String) {
										localDate = simpleDateFormat.parse(String.valueOf(value));
									} else {
										localDate = simpleDateFormat.parse(simpleDateFormat.format(value));
									}

									if (localDate != null) {
										java.sql.Timestamp sqlDate = new java.sql.Timestamp(localDate.getTime());
										value = sqlDate;
									}
								} catch (Exception ex) {
									Date localDate = simpleDateFormat.parse(simpleDateFormat.format(new Date()));
									if (localDate != null) {
										java.sql.Timestamp sqlDate = new java.sql.Timestamp(localDate.getTime());
										value = sqlDate;
									}
								}

							}
						}

					} catch (Exception e) {
					}

				} else if ("NUMBER".equalsIgnoreCase(columnType) || "NUMERIC".equalsIgnoreCase(columnType)
						|| "INTEGER".equalsIgnoreCase(columnType) || "INT".equalsIgnoreCase(columnType)
						|| "BIGINT".equalsIgnoreCase(columnType) || "TINYINT".equalsIgnoreCase(columnType)
						|| "SMALLINT".equalsIgnoreCase(columnType) || "MEDIUMINT".equalsIgnoreCase(columnType)) {

					BigInteger integerObj = null; // // ravi etl integration
					try {
						integerObj = new BigInteger(String.valueOf(value));

					} catch (Exception e) {
						value = 0;
					}
					if (integerObj != null) {
						value = integerObj.intValue();
					}

//                    BigInteger integerObj = new BigInteger(String.valueOf(value));
//                    if (integerObj != null) {
//                        value = integerObj.intValue();
//                    }
				} else if ("FLOAT".equalsIgnoreCase(columnType) || "FLOAT(24)".equalsIgnoreCase(columnType)
						|| "DECIMAL".equalsIgnoreCase(columnType) || "DOUBLE".equalsIgnoreCase(columnType)) {

					BigInteger integerObj = null; // ravi etl integration
					try {
						integerObj = new BigInteger(String.valueOf(value));

					} catch (Exception e) {
						value = 0;
					}
					if (integerObj != null) {
						value = integerObj.intValue();
					}

//                    BigDecimal integerObj = new BigDecimal(String.valueOf(value));
//                    if (integerObj != null) {
//                        value = integerObj.doubleValue();
//                    }
				} else if ("VARCHAR".equalsIgnoreCase(columnType) || "VARCHAR2".equalsIgnoreCase(columnType)) {

					value = String.valueOf(value);
				} else if ("CLOB".equalsIgnoreCase(columnType)) {

					value = new PilogUtilities().clobToString((Clob) value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}

	@Transactional
	public List tableColumnList(HttpServletRequest request, String tablename) {
		List ColumnList = new ArrayList();
		try {
			if (tablename != null && !"".equalsIgnoreCase(tablename)) {
				String tableColumnQuery = "select COLUMN_NAME from all_tab_columns where table_name = '"
						+ tablename.toUpperCase() + "'";
				ColumnList = access.sqlqueryWithParams(tableColumnQuery, Collections.EMPTY_MAP);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ColumnList;
	}

	@Transactional
	public JSONObject deleteTableColumn(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("columnName");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				String tableColumnQuery = "ALTER TABLE " + tableName + " DROP (" + columnName + ")";
				int count = access.executeNativeUpdateSQLWithSimpleParamsNoAudit(tableColumnQuery,
						Collections.EMPTY_MAP);
				resultObj.put("Message", "Column(s) Deleted Successfully");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject mergeformdata(HttpServletRequest request) {
		String result = "";
		Connection connection = null;
		String tabelname = request.getParameter("tablename");
		JSONObject resultobj = new JSONObject();
		try {
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tabelname + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				result = "<div  class='mergeformClass' id='mergeformClassId'>";
//                result +="<span class='visionVisualizeCardValuesSpanClass'>Select Column(s) for Merge</span><br>";
				result += "<div  class='columnlist' id='columnlistId'>"
						+ "<div class='mergetitle'>select Column for Merge</div>";
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					String id = tabelname + "_" + columnName;
					result += "<input type='checkbox' class='visionVisualizeChartFiltersValuesCheckBox' name='visionVisualizeChartFiltersValuesCheckName' value='"
							+ columnName + "'>" + columnName + "</input>";
					result += "<br>";
				}
				result += "</div>";
				result += "<div  class='seperatorListClass' id='seperatorListId'>";

				resultobj.put("columnobj", result);
				JSONObject delimiterobj = new JSONObject();
				delimiterobj.put("Colon", "");
				delimiterobj.put("Comma", ",");
				delimiterobj.put("Equal Sign", "=");
				delimiterobj.put("Semicolon", ";");
				delimiterobj.put("Space", " ");
				delimiterobj.put("--Custome--", "Custome");
				JSONObject caseobj = new JSONObject();
//                caseobj.put("--Select", "SELECT");
				caseobj.put("UpperCase", "UPPER");
				caseobj.put("LowerCase", "LOWER");
				String SeperatorResult = "";
				result += "<span class='visionVisualizeCardValuesSpanClass'>Select or Enter Delimiter</span><br>";
				result += "<select id ='DelimiteDropDownId' class='DxpdashbordoptionListClass' onChange=\"getcustometext(event,id)\"><br>";
				for (Object key : delimiterobj.keySet()) {
					String keyStr = (String) key;
					Object keyvalue = delimiterobj.get(keyStr);
					result += "<option value= '" + keyvalue + "'>" + keyStr + "</option>";
				}

				result += "</select>";
				result += "<input type=text id='customeValId' class='customeValclass' style='display: none;'><br>";
				result += "<span class='visionVisualizeCardValuesSpanClass'>Create New Column</span><br>";
				result += "<input type=text id='createColumnId' class='createColumnClass'><br>";
				result += "<span class='visionVisualizeCardValuesSpanClass'>select Case :</span><br>";
				result += "<select id ='CasesensetiveId' class='CasesensetiveIdClass' onChange=\"changeTextCase(event,id)\"><br>";
				result += "<option value= 'Select'>--Select--</option>";
				for (Object key : caseobj.keySet()) {
					String keyStr = (String) key;
					Object keyvalue = caseobj.get(keyStr);
					result += "<option value= '" + keyvalue + "'>" + keyStr + "</option>";
				}
				result += "</select>";

				result += "</div>";
				result += "</div>";
				resultobj.put("seperatorobj", result);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	
public String transformdata(HttpServletRequest request) {
			                Connection connection = null;
			                try {
			                        JSONArray columnarr = new JSONArray();
			                        Class.forName(dataBaseDriver);
			                        connection = DriverManager.getConnection(dbURL, userName, password);

			                        String tablename = request.getParameter("tablename");
			                        String Columnvalue = request.getParameter("Columnvalue");
			                        String seperator = request.getParameter("seperator");
			                        String columnname = request.getParameter("Columnname");
			                        String CaseVal = request.getParameter("CaseVal");
			                        if (Columnvalue != null && !"".equalsIgnoreCase(Columnvalue) && !"null".equalsIgnoreCase(Columnvalue)) {
			                                columnarr = (JSONArray) JSONValue.parse(Columnvalue);
			                        }
			                        // Check if the column already exists
			                        String checkQuery = "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?";
			                        PreparedStatement checkStatement = connection.prepareStatement(checkQuery);
			                        checkStatement.setString(1, tablename);
			                        checkStatement.setString(2, columnname);
			                        ResultSet resultSet = checkStatement.executeQuery();
			                        resultSet.next();
			                        int columnCount = resultSet.getInt(1);

			                        if (columnCount == 0) {
			                            // Column doesn't exist, so proceed with adding it
			                            String insertQuery = "ALTER TABLE " + tablename + " ADD " + columnname + " VARCHAR2(3000)";
			                            int alterCount = access.executeUpdateSQL(insertQuery, Collections.EMPTY_MAP);
			                            // Handle the alterCount result as needed
			                        } 
			                        if(CaseVal.equals("Select")) {
			                                 String updateQuery = "UPDATE " + tablename + " SET " + columnname + " = (";
			                                    for (int i = 0; i < columnarr.size(); i++) {
			                                        if (i > 0) {
			                                            updateQuery += " || '" + seperator + "' || ";
			                                        }
			                                        updateQuery += columnarr.get(i);
			                                    }
			                                    updateQuery += ")";
			                                    System.out.println("updatedQuery in select : " + updateQuery);
			                                    int updateCount = access.executeUpdateSQL(updateQuery, Collections.EMPTY_MAP);
			                                  
			                                    System.out.println("updateCount: " + updateCount);
			                        }
			                        if(CaseVal.equals("UPPER")) {
			                                 String updateQuery = "UPDATE " + tablename + " SET " + columnname + " = UPPER(";
			                                    for (int i = 0; i < columnarr.size(); i++) {
			                                        if (i > 0) {
			                                            updateQuery += " || '" + seperator + "' || ";
			                                        }
			                                        updateQuery += columnarr.get(i);
			                                    }
			                                    updateQuery += ")";
			                                    System.out.println("updatedQuery in upper: " + updateQuery);
			                                    int updateCount = access.executeUpdateSQL(updateQuery, Collections.EMPTY_MAP);
			                                    System.out.println("updateCount: " + updateCount);
			                                    
			                        }
			                        if (CaseVal.equals("LOWER")) {
			                            String updateQuery = "UPDATE " + tablename + " SET " + columnname + " = LOWER(";
			                            for (int i = 0; i < columnarr.size(); i++) {
			                                if (i > 0) {
			                                    updateQuery += " || '" + seperator + "' || ";
			                                }
			                                updateQuery += columnarr.get(i);
			                            }
			                            updateQuery += ")";
			                            System.out.println("updatedQuery in lower : " + updateQuery);
			                            int updateCount = access.executeUpdateSQL(updateQuery, Collections.EMPTY_MAP);
			                            System.out.println("updateCount: " + updateCount);
			                            
			                        }

			                        
			                        

			                } catch (Exception e) {
			                        e.printStackTrace();
			                }
			                return "new Columns and data created successfully";

			        }

			        



	@Transactional
	public String gettransposedata(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		String result = "";

		try {
			result += "<table class='table-bordered'><tr>";
			String tablename = (String) request.getParameter("tablename");
			String query = "SELECT * FROM " + tablename + " ";
			List tabledata = access.sqlqueryWithParams(query, new HashMap<>());
			System.out.println("tabledata" + tabledata);
			List headerData = columnHeader(request, tablename);
			if (headerData != null && !headerData.isEmpty()) {
				for (int i = 0; i < headerData.size(); i++) {
					result += "<th>" + headerData.get(i) + "</th>";
					if (tabledata != null && !tabledata.isEmpty()) {
						for (int j = 0; j < tabledata.size(); j++) {
							Object[] rowData = (Object[]) tabledata.get(j);
							JSONArray dataArr = new JSONArray();
							JSONObject dataobj = new JSONObject();
							if (rowData != null) {
								for (int k = 0; k < rowData.length; k++) {
									dataArr.add(rowData[k]);
									if (i == k) {
										dataobj.put(headerData.get(i), rowData[k]);
										result += "<th>" + rowData[k] + "</th>";
									}
								}
								dataArray.add(dataobj);

							}
//                
						}
						result += "</tr>";
					}

				}
				result += "</table>";
//                dataArr.add(dataobj);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	@Transactional
	public JSONObject DimensionTransposedata(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONArray columnarr = new JSONArray();
		JSONArray dataarray = new JSONArray();
		List ColumnNameList = new ArrayList();
		String result = "";
		String columnName = "";
		try {
			String transposeColumn = request.getParameter("Values");
			if (transposeColumn != null && !"".equalsIgnoreCase(transposeColumn)
					&& !"null".equalsIgnoreCase(transposeColumn)) {
				columnarr = (JSONArray) JSONValue.parse(transposeColumn);
			}

			String tablename = (String) request.getParameter("tablename");
			result += "<table id ='tansposetableId' class='table-bordered'>";
			if (columnarr != null && !columnarr.isEmpty()) {
				int i;
				result += "<thead><tr>";
				for (i = 0; i < columnarr.size(); i++) {
					columnName = (String) columnarr.get(i);
					ColumnNameList.add(columnName);
					if (columnName != null) {
						result += "<th>" + columnName + "</th>";
					} else {
						result += "<th>Properties</th>";
						result += "<th>Values</th>";
						ColumnNameList.add("PROPERTY_NAME");
						ColumnNameList.add("VALUE");
					}
				}
				result += "<th><input type='text' contenteditable='true' id='propertiesId'></th>";
				result += "<th><input type='text' contenteditable='true' id='ValuesId'></th>";
				ColumnNameList.add("PROPERTY_NAME");
				ColumnNameList.add("VALUE");
				result += "</tr></thead><tbody>";
				String query = "SELECT * FROM " + tablename + " ";
				List ColumnDtadata = access.sqlqueryWithParams(query, new HashMap<>());
				List headerData = columnHeader(request, tablename);
				int loopcount = columnarr.size() + 2;
				int loopSize = headerData.size() - columnarr.size();
				if (ColumnDtadata != null && !ColumnDtadata.isEmpty()) {
					for (int j = 0; j < ColumnDtadata.size(); j++) {
						Object[] rowData = (Object[]) ColumnDtadata.get(j);
						if (headerData != null && !headerData.isEmpty()) {
							int count = i;
							for (int l = 0; l < loopSize; l++) {
								List ColumnList = new ArrayList();
								for (int k = 0; k < loopcount - 1; k++) {
									if (k < i) {
										result += "<td>" + rowData[k] + "</td>";
										ColumnList.add(rowData[k]);
									} else {
										result += "<td>" + headerData.get(count) + "</td>";
										ColumnList.add(headerData.get(count));
										result += "<td>" + rowData[count] + "</td>";
										ColumnList.add(rowData[count]);
									}

								}
								dataarray.add(ColumnList);
								count++;
								result += "</tr>";
							}
						}
					}
					result += "</tbody></table>";
				}
			}
			result += "</table>";
			resultobj.put("result", result);
			resultobj.put("data", dataarray);
			resultobj.put("ColumnName", ColumnNameList);
			System.out.println("resultobj" + dataarray);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;

	}

	public List columnHeader(HttpServletRequest request, String tablename) {
		ArrayList HeaderList = new ArrayList();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection connection = null;

		try {
			String selectQuery = "SELECT * FROM " + tablename;
			connection = DriverManager.getConnection(dbURL, userName, password);
			preparedStatement = connection.prepareStatement(selectQuery);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			int columnCount = resultSetMetaData.getColumnCount();
			for (int i = 1; i <= columnCount; i++) {
				String columnName = resultSetMetaData.getColumnName(i);
				HeaderList.add(columnName);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return HeaderList;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String caseSensitive(HttpServletRequest request) {
		JSONArray columnarr = new JSONArray();
		String columnname = "";
		String sign = "=";
		String message = "";
		try {
			String Columnvalue = request.getParameter("Columnvalue");
			String CaseVal = request.getParameter("CaseVal");
			String tablename = request.getParameter("tablename");
			if (CaseVal != null && !"".equalsIgnoreCase(CaseVal) && tablename != null
					&& !"".equalsIgnoreCase(tablename)) {
				try {
					if (Columnvalue != null && !"".equalsIgnoreCase(Columnvalue)
							&& !"null".equalsIgnoreCase(Columnvalue)) {
						columnarr = (JSONArray) JSONValue.parse(Columnvalue);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (columnarr != null && !columnarr.isEmpty()) {
					for (int i = 0; i < columnarr.size(); i++) {
						String columnName = (String) columnarr.get(i);
						columnname = columnName + "=" + CaseVal + "(" + columnName + ")";
						if (i != columnarr.size() - 1) {
							columnname += ",";
						}
					}
					String updatequery = "update " + tablename + " set " + columnname;
					System.out.println("updatequery::::::::::::" + updatequery);
					int updateCount = access.executeUpdateSQL(updatequery, Collections.EMPTY_MAP);
					System.out.println("updateCount" + updateCount);

				} else {

					columnname = Columnvalue + "=" + CaseVal + "(" + Columnvalue + ")";
					String updatequery = "update " + tablename + " set " + columnname;
					System.out.println("updatequery::::::::::::" + updatequery);
					int updateCount = access.executeUpdateSQL(updatequery, Collections.EMPTY_MAP);
					System.out.println("updateCount" + updateCount);
					if (updateCount > 0) {
						message = "updated SuccessFully";
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	@Transactional
	public String DimensionTransposeColumn(HttpServletRequest request) {
		String result = "";
		Connection connection = null;
		String tabelname = request.getParameter("tablename");
		JSONObject resultobj = new JSONObject();
		try {
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tabelname + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				result = "<div  class='DimensiontransposeClass' id='DimensiontransposeId'>";
//                result +="<span class='visionVisualizeCardValuesSpanClass'>select Column for Merge</span><br>";
				result += "<div  class='TransposeColumnClass' id='TransposeColumnId'>"
						+ "<div class='mergetitle'>Select Transpose Column</div>";
				int count = 1;
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					String id = tabelname + "_" + columnName;
					if (i == 1) {
						result += "<input type='checkbox' id ='visionVisualizeTransposeCheckBoxId" + i
								+ "' class='visionVisualizeChartTransposeCheckBox' name='visionVisualizeChartFiltersValuesCheckName'  value='"
								+ columnName + "' onclick='stickyheaddsadaer();'>" + columnName + " </input>";
						result += "<br>";
					} else {
						result += "<input type='checkbox' disabled id ='visionVisualizeTransposeCheckBoxId" + i
								+ "' class='visionVisualizeChartTransposeCheckBox' name='visionVisualizeChartFiltersValuesCheckName' value='"
								+ columnName + "' onclick='stickyheaddsadaer();'>" + columnName + "</input>";
						result += "<br>";
					}

				}
				result += "</div>";
				result += "</div>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String createTransposeTable(HttpServletRequest request) {
		PreparedStatement preparedStatement = null;
		JSONArray dataArray = new JSONArray();
		Connection connection = null;
		String message = "";
		int deletecount = 0;
		try {
			String tableName = request.getParameter("tablename");
			String ColumnNameList = request.getParameter("columnList");
			if (ColumnNameList != null && !ColumnNameList.isEmpty()) {
				ColumnNameList = ColumnNameList.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("", "");
			}
			String data = request.getParameter("data");
			if (data != null && !"".equalsIgnoreCase(data) && !"null".equalsIgnoreCase(data)) {
				dataArray = (JSONArray) JSONValue.parse(data);
			}
			List ColumnList = new ArrayList(Arrays.asList(ColumnNameList.split(",")));
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				tableName = "STG_" + tableName;
				String deleteQuery = "DROP TABLE " + tableName + "";
				try {
					deletecount = access.executeUpdateSQL(deleteQuery);
				} catch (Exception e) {
					e.printStackTrace();
				}

				String createTableQuery = " CREATE TABLE " + tableName + "( ";
				if (ColumnList != null && !ColumnList.isEmpty()) {
					for (int i = 0; i < ColumnList.size(); i++) {
						if (i < (ColumnList.size() - 1)) {

							createTableQuery += (String) ColumnList.get(i) + " VARCHAR2(4000) , ";
						} else {
							createTableQuery += (String) ColumnList.get(i) + " VARCHAR2(4000))";
						}
					}
					int count = access.executeUpdateSQL(createTableQuery);
					String query = "";
					String columnsStr = (String) ColumnList.stream().map(e -> e).collect(Collectors.joining(","));
					String paramsStr = (String) ColumnList.stream().map(e -> "?").collect(Collectors.joining(","));
					query = "INSERT INTO " + tableName + " (" + columnsStr + " ) VALUES ( " + paramsStr + ")";

					if (query != null && !"".equalsIgnoreCase(query) && dataArray != null && !dataArray.isEmpty()) {
						connection = DriverManager.getConnection(dbURL, userName, password);
						preparedStatement = connection.prepareStatement(query);
						for (int i = 0; i < dataArray.size(); i++) {
							JSONArray dataArr = (JSONArray) dataArray.get(i);
							for (int j = 0; j < dataArr.size(); j++) {
								preparedStatement.setObject(j + 1, dataArr.get(j));
							}
							preparedStatement.addBatch();
						}
						int[] datacount = preparedStatement.executeBatch();
						if (datacount != null && datacount.length > 0) {
							message = "<span><h6>(" + datacount.length + ") Rows Executed SuccessFully<h6><span>";
						} else {
							message = "unable to Insert Data";
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;

	}

	@Transactional
	public boolean setImportData(HttpServletRequest request) {
		boolean updateStatus = false;
		try {
			String tablename = request.getParameter("tablename");
			String dataColumn = request.getParameter("dataColumn");
			String dataTypeOracal = request.getParameter("dataTypeOrecal");
			String dataSize = request.getParameter("dataSize");
			String dataInputType = request.getParameter("dataInputType");
			String selectQuery = "ALTER TABLE " + tablename + " MODIFY " + dataColumn + " " + dataTypeOracal + " ("
					+ dataInputType + ")";
			access.executeNativeUpdateSQLWithSimpleParamsNoAudit(selectQuery, Collections.EMPTY_MAP);
			updateStatus = true;
		} catch (Exception e) {
			System.out.println(e);
		}
		return updateStatus;
	}

	@Transactional
	public JSONObject showtableData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONArray dataFieldsArray = new JSONArray();
		JSONArray columnsArray = new JSONArray();

		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection connection = null;

		int recordsCount = 0;
		try {
			String groupscount = request.getParameter("groupscount");
			String tableName = request.getParameter("tableName");
			tableName = tableName.replaceAll("[^a-zA-Z0-9_]", "_");

//            String tableName = "TIMESHEET_RECORD_TEMPLATE";

			String pagenum = request.getParameter("pagenum");
			String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize") : "10";
			String recordendindex = request.getParameter("recordendindex");
			String recordstartindex = (request.getParameter("recordstartindex"));

			String getOnlyDataArray = (request.getParameter("getOnlyDataArray"));
			connection = DriverManager.getConnection(dbURL, userName, password);
			int startIndex = 0;
			int endIndex = 0;
			if (recordstartindex != null && recordendindex != null && pagesize != null) {
				startIndex = Integer.parseInt(recordstartindex);
				endIndex = Integer.parseInt(recordendindex);
			}

			String conditionQuery = "";

			Integer filterscount = 0;
			String filterCondition = "";
			String selectQuery = "SELECT * FROM " + tableName;

			String countQuery = "SELECT count(*) FROM (" + selectQuery + " )";
			ResultSet countResultSet = connection.prepareStatement(countQuery).executeQuery();
			while (countResultSet.next()) {
				recordsCount = countResultSet.getInt(1);

			}

			String orderby = "";
			String sortdatafield = request.getParameter("sortdatafield");
			System.out.println("sortdatafield::::" + sortdatafield);
			String sortorder = request.getParameter("sortorder");
			if (!(sortdatafield != null && !"".equalsIgnoreCase(sortdatafield))) {
				sortdatafield = (String) request.getAttribute("sortdatafield");
			}
			if (!(sortorder != null && !"".equalsIgnoreCase(sortorder))) {
				sortorder = (String) request.getAttribute("sortorder");
			}
			System.out.println("sortorder::::" + sortorder);
			if (sortdatafield != null && sortorder != null && (sortorder.equals("asc") || sortorder.equals("desc"))) {
				orderby = " ORDER BY " + sortdatafield + " " + sortorder;
			}

			selectQuery += orderby;
			if (dataBaseDriver.toUpperCase().contains("ORACLE")) {
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			} else if (dataBaseDriver.toUpperCase().contains("MYSQL")) {
				conditionQuery += " LIMIT " + startIndex + "," + pagesize + "";
			} else if (dataBaseDriver.toUpperCase().contains("MSSQL")) {
				if (!(orderby != null && !"".equalsIgnoreCase(orderby) && !"null".equalsIgnoreCase(orderby))) {
					selectQuery += " ORDER BY (SELECT NULL) ";
				}
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			}

			selectQuery = selectQuery + conditionQuery;

			System.out.println("Tree Data query::" + selectQuery);
			preparedStatement = connection.prepareStatement(selectQuery);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			int columnCount = resultSetMetaData.getColumnCount();

			if (getOnlyDataArray != null && "Y".equalsIgnoreCase(getOnlyDataArray)) {

				while (resultSet.next()) {
					JSONObject dataObj = new JSONObject();

					for (int i = 1; i <= columnCount; i++) {
						JSONObject dataFieldsObj = new JSONObject();
						String columnType = resultSetMetaData.getColumnTypeName(i);
						String columnName = resultSetMetaData.getColumnName(i);
						Object data = null;
						if ("DATE".equalsIgnoreCase(columnType) || "DATETIME".equalsIgnoreCase(columnType)
								|| "TIMESTAMP".equalsIgnoreCase(columnType)) {
							data = resultSet.getString(columnName);
						} else {
							data = resultSet.getObject(columnName);
						}
						if (data instanceof byte[]) {
							byte[] bytesArray = (byte[]) data;
							data = new RAW(bytesArray).stringValue();
						}
						dataObj.put(columnName, data);

					}

					dataArray.add(dataObj);

				}
				if (recordsCount != 0) {
					dataArray.add(recordsCount);
				}

				resultObj.put("dataArray", dataArray);
			} else {
				List<String> columnList = new ArrayList();
				for (int i = 1; i <= columnCount; i++) {

					JSONObject dataFieldsObj = new JSONObject();
					String columnType = resultSetMetaData.getColumnTypeName(i);
					String columnName = resultSetMetaData.getColumnName(i);
					columnList.add(columnName);
					dataFieldsObj.put("name", columnName);
					dataFieldsObj.put("type", "string");

					dataFieldsArray.add(dataFieldsObj);

					JSONObject columnsObject = new JSONObject();

					columnsObject.put("text", columnName);
					columnsObject.put("datafield", columnName);
					columnsObject.put("width", 120);
					columnsObject.put("sortable", true);
					columnsArray.add(columnsObject);

				}

				resultObj.put("dataFieldsArray", dataFieldsArray);
				resultObj.put("columnsArray", columnsArray);
				resultObj.put("columnList", columnList);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}

	@Transactional
	public String gettableattribute(HttpServletRequest request) {
		String result = "";
		Connection connection = null;
		String tabelname = request.getParameter("tablename");
		JSONObject resultobj = new JSONObject();
		try {
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tabelname + "");
			String resultst = "";
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				result = "<div  class='DimensiontransposeClass' id='DimensiontransposeId' style =\"display:flex;gap:2px;\">";// saqu
//                result +="<span class='visionVisualizeCardValuesSpanClass'>select Column for Merge</span><br>";
				result += "<div  class='TransposeColumnClass' id='TransposeColumnId'>"
						+ "<div class='mergetitle'></div>";

//                  resultst +=access.executeUpdateSQL(String ,"ALTER TABLE sample2 ALTER COLUMN DataType datatype");     
				int count = 1;
				String str = "";
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					String id = tabelname + "_" + columnName;

					if (i == 1) {
//                        result += "<input  id ='visionVisualizeTransposeCheckBoxId" + i + "' class='visionVisualizeChartTransposeCheckBox' name='visionVisualizeChartFiltersValuesCheckName'  value='" + columnName + "' onclick='stickyheaddsadaer();'> </input>";
//                        result += "<br>";
						str += " <option id='selectOptionAnlysisData1'\">" + columnName + " select colum</option>";
						str += "<br>";
					} else {
//                        result += "<input  disabled id ='visionVisualizeTransposeCheckBoxId" + i + "' class='visionVisualizeChartTransposeCheckBox' name='visionVisualizeChartFiltersValuesCheckName' value='" + columnName + "' onclick='stickyheaddsadaer();'></input>";
//                        result += "<br>";
						str += " <option id='selectOptionAnlysisData1'\">" + columnName + "</option>";
						str += "<br>";
					}
				}
				result += "</div>";
				result += "<div id='selectOptionAnlysisData1'><select>" + str + "</select></div>";
				result += "<div id='selectOptionAnlysisData'></div>";
				result += "<div id='selectOptionDataType'></div>";
				result += "<div id='selectOptionDataTypeInput' class='selectOptionDataTypeInput'><input type=\"number\"id=\"quantity\"name=\"quantity\" min=\"1\" max=\"400\'style =\" placeholder=\"Size\"></div>";
				result += "</div>";
				result += "<div id='dimensionTranspose' style='display:none;'></div>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public List gettableList(HttpServletRequest request) {
		List Tablelist = new ArrayList();
		try {
			String tableQuery = "SELECT TABLE_NAME FROM TABS ORDER BY TABLE_NAME ";
			Tablelist = access.sqlqueryWithParams(tableQuery, Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Tablelist;
	}

	@Transactional
	public List gettablecolumn(HttpServletRequest request) {
		List columnlist = new ArrayList();
		try {
			String table = request.getParameter("tablename");
			String tableQuery = "SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME ='" + table + "'";
			columnlist = access.sqlqueryWithParams(tableQuery, Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return columnlist;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public JSONObject getModalFileColumns(HttpServletRequest request) {
		Connection connection = null;
		JSONObject resultObj = new JSONObject();
		List stringList = new ArrayList();
		List numberList = new ArrayList();
		List remarksArr = new ArrayList();
		List questionsArr = new ArrayList();
		try {
			String tableName = request.getParameter("tableName");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				MultiValueMap<String, String> inputMap = new LinkedMultiValueMap<>();
				JSONObject dbDetails = new PilogUtilities().getDatabaseDetails(dataBaseDriver, dbURL, userName, password, "DH101102");
				inputMap.add("table_name", tableName);
				inputMap.add("USER_NAME", userName);
				inputMap.add("PASSWORD", password);
				inputMap.add("HOST", (String) dbDetails.get("HOST_NAME"));
				inputMap.add("PORT", (String) dbDetails.get("CONN_PORT"));
				inputMap.add("SERVICE_NAME", (String) dbDetails.get("CONN_DB_NAME"));
				HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(
						inputMap, headers);
				RestTemplate template = new RestTemplate();
				ResponseEntity<JSONObject> response = template
						.postForEntity("http://apihub.pilogcloud.com:6654/chart_datatypes/", entity, JSONObject.class);
				JSONObject apiDataObj = response.getBody();
				if (apiDataObj != null && !apiDataObj.isEmpty()) {
					Set keySet = apiDataObj.keySet();
					for (Object str : keySet) {
						String keyName = (String) str;
						if (keyName != null && !"".equalsIgnoreCase(keyName) && "Remarks".equalsIgnoreCase(keyName)) {
							remarksArr = (List) apiDataObj.get(keyName);
						}else if(keyName !=null && !"".equalsIgnoreCase(keyName) && "questions".equalsIgnoreCase(keyName))
							
						{
							Map questionsMap = (Map) apiDataObj.get(keyName);
							if(questionsMap !=null && !questionsMap.isEmpty())
								{
								questionsMap.entrySet().forEach(e->{
									questionsArr.add(((Entry)e).getKey());
								});
								resultObj.put("querysMap", questionsMap);
								}
						}
						else {
							String value = (String) apiDataObj.get(keyName);
							if (value != null && !"".equalsIgnoreCase(value) && "NUMBER".equalsIgnoreCase(value)) {
								numberList.add(keyName);
							} else if (value != null && !"".equalsIgnoreCase(value)
									&& "VARCHAR2".equalsIgnoreCase(value)) {
								stringList.add(keyName);
							} else if (value != null && !"".equalsIgnoreCase(value) && "DATE".equalsIgnoreCase(value)) {
								stringList.add(keyName);
							}
						}
					}
				}
			}

			/*
			 * if (tableName != null && !"".equalsIgnoreCase(tableName)) { connection =
			 * dashboardutils.getCurrentConnection(); Statement statement =
			 * connection.createStatement(); ResultSet results =
			 * statement.executeQuery("SELECT * FROM " + tableName + ""); ResultSetMetaData
			 * metadata = results.getMetaData(); int columnCount =
			 * metadata.getColumnCount(); if (columnCount > 0) { for (int i = 1; i <=
			 * columnCount; i++) { String columnName = metadata.getColumnName(i); String
			 * columnType = metadata.getColumnTypeName(i); String id = tableName + "." +
			 * columnName; if (columnType != null && !"".equalsIgnoreCase(columnType) &&
			 * "NUMBER".equalsIgnoreCase(columnType)) { numberList.add(id); } else if
			 * (columnType != null && !"".equalsIgnoreCase(columnType) &&
			 * "VARCHAR2".equalsIgnoreCase(columnType)) { stringList.add(id); } else if
			 * (columnType != null && !"".equalsIgnoreCase(columnType) &&
			 * "DATE".equalsIgnoreCase(columnType)) { stringList.add(id); } }
			 * 
			 * } }
			 */

			resultObj.put("stringList", stringList);
			resultObj.put("numberList", numberList);
			resultObj.put("remarksArr", remarksArr);
			resultObj.put("questionsArr", questionsArr);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	public List getColumnTypesOfImportedFile(HttpServletRequest request, HttpServletResponse response,
			String filePath) {
		List headerTypeList = new ArrayList();
		try {
			if (filePath != null && !"".equalsIgnoreCase(filePath) && !"null".equalsIgnoreCase(filePath)) {
				String fileExt = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());

				if (fileExt != null && !"".equalsIgnoreCase(fileExt)) {
					if ("xls".equalsIgnoreCase(fileExt) || "xlsx".equalsIgnoreCase(fileExt)) {
						Workbook workBook = null;
						Sheet sheet = null;
						String sheetNum = request.getParameter("sheetNo");
						int sheetNo = (sheetNum != null && !"".equalsIgnoreCase(sheetNum))
								? (Integer.parseInt(sheetNum))
								: 0;

						System.out.println("Before::::" + new Date());
						workBook = WorkbookFactory.create(new File(filePath));
						System.out.println("After::fileInputStream::" + new Date());
						sheet = (XSSFSheet) workBook.getSheetAt(sheetNo);
						if (sheet != null) {
							Row row = sheet.getRow(1);
							if (row != null) {
								for (int j = 0; j < row.getLastCellNum(); j++) {
									try {
										Cell cell = row.getCell(j);

										if (cell != null) {
											switch (cell.getCellType()) {
												case Cell.CELL_TYPE_STRING:
													String dataFormatString = cell.getCellStyle().getDataFormatString();
													headerTypeList.add("VARCHAR2");
													break;
												case Cell.CELL_TYPE_BOOLEAN:
//                                rowObj.put(header, hSSFCell.getBooleanCellValue());
													break;
												case Cell.CELL_TYPE_NUMERIC:

													if (HSSFDateUtil.isCellDateFormatted(cell)) {
														String cellDateString = "";
														Date cellDate = cell.getDateCellValue();
														if ((cellDate.getYear() + 1900) == 1899
																&& (cellDate.getMonth() + 1) == 12
																&& (cellDate.getDate()) == 31) {
															cellDateString = (cellDate.getHours()) + ":"
																	+ (cellDate.getMinutes()) + ":"
																	+ (cellDate.getSeconds());
//                                                    System.out.println("cellDateString :: "+cellDateString);
														} else {
															cellDateString = (cellDate.getYear() + 1900) + "-"
																	+ (cellDate.getMonth() + 1) + "-"
																	+ (cellDate.getDate());
														}

//                                                        String cellDateString = (cellDate.getYear() + 1900) + "-" + (cellDate.getMonth() + 1) + "-" + (cellDate.getDate());
														headerTypeList.add("DATE");
//                                            
													} else {
														String cellvalStr = NumberToTextConverter
																.toText(cell.getNumericCellValue());
														headerTypeList.add("NUMBER");
													}
													break;
												case Cell.CELL_TYPE_BLANK:
													String headerType = "";
													headerType = getBlankCellHeaderType(sheet, 2, j, headerType);
													headerTypeList.add(headerType);
													break;
											}

										} else {
											String headerType = "";
											headerType = getBlankCellHeaderType(sheet, 2, j, headerType);
											headerTypeList.add(headerType);
										}
									} catch (Exception e) {
										e.printStackTrace();
										headerTypeList.add("");
										continue;
									}

								} // end of row cell loop
							}
						}

					} else if ("txt".equalsIgnoreCase(fileExt) || "csv".equalsIgnoreCase(fileExt)) {
						CsvParserSettings settings = new CsvParserSettings();
						settings.detectFormatAutomatically();

						CsvParser parser = new CsvParser(settings);
						List<String[]> rows = parser.parseAll(new File(filePath));

						// if you want to see what it detected
//                        CsvFormatDetector formatdetect =  new CsvFormatDetector();
						char columnSeparator = ',';
						String fileType = request.getParameter("fileType");
//                        char columnSeparator = '\t';
//                        char columnSeparator = ',';
						if (!(fileType != null && !"".equalsIgnoreCase(fileType)
								&& !"null".equalsIgnoreCase(fileType))) {
							fileType = (String) request.getAttribute("fileType");
						}
						if (".json".equalsIgnoreCase(fileType)) {
							columnSeparator = ',';
						} else {
							CsvFormat format = parser.getDetectedFormat();
							columnSeparator = format.getDelimiter();
						}

						CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF8"),
								columnSeparator);

						String[] nextLine;
						reader.readNext();
						List<String> values = null;
						while ((nextLine = reader.readNext()) != null) {
							if (nextLine.length != 0 && nextLine[0].contains("" + columnSeparator)) {
								values = new ArrayList<>(Arrays.asList(nextLine[0].split("" + columnSeparator)));
							} else {
								values = new ArrayList<>(Arrays.asList(nextLine));
							}

							break;
						}
						for (int i = 0; i < values.size(); i++) {
							String value = values.get(i);

							int dataTypeLength = value.length();
							String dataType = getOracleDataTypeOfValue(value, dataTypeLength);
							headerTypeList.add(dataType);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return headerTypeList;
	}

	@Transactional
	public JSONObject fetchModalChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		JSONObject filteredChartConfigObj = new JSONObject();
		try {
			List selectData = null;
			List<String> columnKeys = new ArrayList<>();
			JSONObject chartConfigObj = new JSONObject();
			String chartId = request.getParameter("chartId");
			String chartType = request.getParameter("chartType");
			String axisColumnName = request.getParameter("axisColumnName");
			String chartConfigObjStr = request.getParameter("chartPropObj");
			String script = request.getParameter("script");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			JSONObject chartListObj = new JSONObject();
			if (script != null && !"".equalsIgnoreCase(script)) {
				chartListObj = getScriptModalChartDataList(request);
			} else {
				chartListObj = getModalChartDataList(request);
			}

			int totalChartCount = 0;
			if (chartListObj != null && !chartListObj.isEmpty()) {
				selectData = (List) chartListObj.get("chartList");
				columnKeys = (List<String>) chartListObj.get("columnKeys");
				if (chartListObj.get("totalChartCount") != null) {
					totalChartCount = (int) chartListObj.get("totalChartCount");
					chartObj.put("totalChartCount", totalChartCount);
				}

			}
			JSONObject framedChartDataObj = getFramedChartDataObject(request, selectData, columnKeys, layoutObj,
					dataPropObj, chartType);
			if (framedChartDataObj != null && !framedChartDataObj.isEmpty()) {
				chartObj.put("layout", (JSONObject) framedChartDataObj.get("layoutObj"));
				if (chartType != null && !"".equalsIgnoreCase(chartType) && "indicator".equalsIgnoreCase(chartType)) {
					JSONObject indicatorObj = getIndicatorDataObject(framedChartDataObj, columnKeys);
					if (indicatorObj != null && !indicatorObj.isEmpty()) {
						JSONArray gaugeDataArr = new JSONArray();
						gaugeDataArr.add(indicatorObj.get("data"));
						chartObj.put("data", gaugeDataArr);
						chartObj.put("gauge", indicatorObj.get("gauge"));
					}
				} else {
					chartObj.put("data", (JSONObject) framedChartDataObj.get("dataObj"));
				}
			}
			chartObj.put("dataPropObject", dataPropObj);
			chartObj.put("chartId", chartId);
			chartObj.put("axisColumnName", axisColumnName);

//            insertChartDetailsInTable(dataPropObj, dataObj, layoutObj, chartId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public JSONObject getModalChartDataList(HttpServletRequest request) {
		JSONObject chartListObj = new JSONObject();
		try {
			boolean flag = false;
			String selectQuery = "";
			String whereCondQuery = "";
			String groupByCond = "";
			String distinctQuery = "";
			String orderBy = "";
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterConditions");
			String chartPorpObj = request.getParameter("chartPorpObj");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String startIndex = request.getParameter("startIndex");
			String endIndex = request.getParameter("endIndex");
			String pageSize = request.getParameter("pageSize");
			int limit = 0;
			int startLimit = 0;
			if (startIndex != null && !"".equalsIgnoreCase(startIndex) && endIndex != null
					&& !"".equalsIgnoreCase(endIndex)) {
				limit = (int) Integer.parseInt(endIndex);
				startLimit = Integer.parseInt(startIndex);
			}
			List<String> columnKeys = new ArrayList<>();
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			String removeGroupBy = "";
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							String[] columns = columnName.split(",");
							if (columns != null && columns.length > 0) {
								for (int j = 0; j < columns.length; j++) {
									String column = columns[j];
									String[] filteredColumnnameArr = column.split("\\.");
									String filteredColumnname = filteredColumnnameArr[0].replaceAll("\\)", "");
									if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
											&& !"null".equalsIgnoreCase(filteredColumnname)) {
										filteredColumnname = filteredColumnname.replaceAll("_", " ");
										if (filteredColumnname.contains("DATE")) {
											removeGroupBy = "Yes";
										}
									}

									columnKeys.add(filteredColumnname);
									selectQuery += " " + column + ", ";
									groupByCond += column + ", ";

								}
							}
						}
					}
				}
			}
			distinctQuery = selectQuery;
			distinctQuery = new PilogUtilities().trimChar(distinctQuery);
			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						String[] filteredColumnnameArr = columnName.split("\\.");
						String filteredColumnname = "";
						if (filteredColumnnameArr != null && filteredColumnnameArr.length > 1) {
							filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
						}
						if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
								&& !"null".equalsIgnoreCase(filteredColumnname)) {
							filteredColumnname = filteredColumnname.replaceAll("_", " ");
						}

						columnKeys.add(filteredColumnname + "ASCOL" + i);
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {
							selectQuery += " " + columnName + " AS COL" + i + " ,";
							flag = true;
							if (i == 0) {
								orderBy += " COL" + i + " DESC ";
							}
						} else {
							selectQuery += " " + columnName + ", ";
							groupByCond += columnName;
							if (i == 0) {
								orderBy += filteredColumnname + " DESC ";
							}
							if (i < valuesColsArr.size() - 1) {
								groupByCond += ",";
							}
						}
					}
				}

				if (chartType != null && !"".equalsIgnoreCase(chartType)
						&& ("indicator".equalsIgnoreCase(chartType) || "Card".equalsIgnoreCase(chartType))) {
					groupByCond = "";
				} else if (!flag) {
					groupByCond = "";
				} else if (groupByCond != null && !"".equalsIgnoreCase(groupByCond)) {
					groupByCond = new PilogUtilities().trimChar(groupByCond, ',');
					groupByCond = " GROUP BY " + groupByCond;
				}

			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}
			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {

				whereCondQuery = whereCondQuery + " AND " + distinctQuery + " IS NOT NULL ";
			} else {
				whereCondQuery = distinctQuery + " IS NOT NULL ";
			}

			if (orderBy != null && !"".equalsIgnoreCase(orderBy) && !"null".equalsIgnoreCase(orderBy)) {
				orderBy = " ORDER BY " + orderBy;
			}

			if (selectQuery != null && !"".equalsIgnoreCase(selectQuery) && tablesArr != null && !tablesArr.isEmpty()) {

				String tableName = (String) tablesArr.get(0);

				if (tableName != null && !"".equalsIgnoreCase(tableName) && tableName.contains("WHERE")) {
					whereCondQuery = " AND " + whereCondQuery;
				} else {
					whereCondQuery = " WHERE " + whereCondQuery;
				}
				String countQuery = "";
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
					selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
					selectQuery = "SELECT " + selectQuery + " " + tableName + whereCondQuery + groupByCond + orderBy;
					if (removeGroupBy != null && !"".equalsIgnoreCase(removeGroupBy)) {
						countQuery = "SELECT COUNT(*) FROM " + tableName + whereCondQuery;
					} else {
						countQuery = "SELECT COUNT(*) FROM " + tableName + whereCondQuery + groupByCond;
					}

					System.out.println("selectQuery :::" + selectQuery);
				} else {
					selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
					selectQuery = "SELECT " + selectQuery + " FROM " + tableName + whereCondQuery + groupByCond
							+ orderBy;
					if (removeGroupBy != null && !"".equalsIgnoreCase(removeGroupBy)) {
						countQuery = "SELECT COUNT(*) FROM " + tableName + whereCondQuery;
					} else {
						countQuery = "SELECT COUNT(*) FROM " + tableName + whereCondQuery + groupByCond;
					}
					System.out.println("selectQuery :::" + selectQuery);
				}
				int dataCount = 0;
				if (countQuery != null && !"".equalsIgnoreCase(countQuery)) {
					List countData = access.sqlqueryWithParams(countQuery, new HashMap());
					if (countData != null && !countData.isEmpty()) {
						dataCount = countData.size();
						chartListObj.put("totalChartCount", dataCount);
					}
				}
				if (dataCount > 30) {
					distinctQuery = "SELECT DISTINCT(" + distinctQuery + ") FROM " + tableName + whereCondQuery
							+ groupByCond;
					List distinctQueryData = access.sqlqueryWithParams(distinctQuery, new HashMap());
					if (distinctQueryData != null && !distinctQueryData.isEmpty()) {
						int distinctQueryDataCount = distinctQueryData.size();
						if (distinctQueryDataCount > 0) {
							int percent = (distinctQueryDataCount / dataCount) * 100;
							if (percent > 30) {
								return new JSONObject();
							}
						}

					}
				}
			}

			List selectData = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), 10, 0);
			if (selectData != null && !selectData.isEmpty()) {
				chartListObj.put("chartList", selectData);
			}
			if (columnKeys != null && !columnKeys.isEmpty()) {
				chartListObj.put("columnKeys", columnKeys);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chartListObj;
	}

	@Transactional
	public String renameSQLColumn(HttpServletRequest request) {
		String result = "";
		try {
			String renameColumn = request.getParameter("renameColumn");
			String column = request.getParameter("column");
			String table = request.getParameter("table");
			String renameQuery = "ALTER TABLE " + table + " RENAME COLUMN " + column + " TO " + renameColumn + "";
			int count = access.executeUpdateSQL(renameQuery, Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getColumnDataType(HttpServletRequest request) {
		Connection connection = null;
		String result = "";
		try {
			String tableName = request.getParameter("table");
			String column = request.getParameter("column");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				connection = dashboardutils.getCurrentConnection();
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metadata.getColumnName(i);
						String columnType = metadata.getColumnTypeName(i);
						if (column != null && column.equalsIgnoreCase(columnName)) {
							result = "<div id ='AggregateBiColumnmainId'  class='AggregateBiColumnmainClass'>";
							result += "<div class='ColumnRenameClass' ><span class='title'>Renamed Column: </span><span class='inputFeild'><input type='text' style='text-transform:uppercase' id='ColumnRenameid'/></span>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
									+ "<input type='button' value ='Apply' id ='buttonId' onclick=renametableColumn('"
									+ column + "','" + tableName + "')></div></div>";
							result += "</div>";
							result += "<div id ='AggregateBiColumnId'  class='AggregateBiColumnClass'>";
							result += "<span class='title'>Select Type :</span><span class='inputFeild'><select id ='smartBiSelect' class='smartBiSelectClass'"
									+ "onchange=\"getAggregateResult('" + column + "','" + tableName + "')\"></span>";
							if (columnType != null && columnType.equalsIgnoreCase("VARCHAR2")) {
								result += "<option value='SELECT'>Select</option>"
										+ "<option value='COUNT'>COUNT</option>";
							} else if (columnType != null && columnType.equalsIgnoreCase("NUMBER")) {
								result += "<option value='SELECT'>Select</option>"
										+ "<option value='COUNT'>Count</option>" + "<option value='SUM'>Sum</option>"
										+ "<option value='AVG'>Average</option>"
										+ "<option value='MIN'>Minimum</option>"
										+ "<option value='MAX'>Maximum</option>"
										+ "<option value='MEDIAN'>Median</option>";
							}
							result += "</select>";
							result += "</span>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
									+ "<input type='button' value ='Apply' id ='buttonId' onclick=getAggregateResult('"
									+ column + "','" + tableName + "')></div></div>";
							result += "</div>";
							result += "<div id ='sufixId' class='sufixClass'>";
							result += "<div class='ColumnRenameClass' ><span class='title'>Suffix: </span><span class='inputFeild'><input type='text' id='suffixId'/></span>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
									+ "<input type='button' value ='Apply' id ='buttonId' onclick=createSuffixAndPrifix('"
									+ column + "','" + tableName + "')></div></div>";
							result += "</div>";

							result += "<div id ='prifixId' class='prifixClass'>";
							result += "<div class='ColumnRenameClass' ><span class='title'>Prifix: </span><span class='inputFeild'><input type='text'  id='prifixId' value=''/></span>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
									+ "<input type='button' value ='Apply' id ='buttonId' onclick=createPrifix('"
									+ column + "','" + tableName + "')></div></div>";
							result += "</div>";
							JSONObject caseobj = new JSONObject();
							caseobj.put("UpperCase", "UPPER");
							caseobj.put("LowerCase", "LOWER");
							result += "<div id ='CaseId' class='CaseClass'>";
							result += "<span class='visionVisualizeCardValuesSpanClass'>select Case :</span>"
									+ "<select id ='CasesensetiveId' class='CasesensetiveIdClass'>"
									+ "<option value= 'Select'>--Select--</option>";
							for (Object key : caseobj.keySet()) {
								String keyStr = (String) key;
								Object keyvalue = caseobj.get(keyStr);
								result += "<option value= '" + keyvalue + "'>" + keyStr + "</option>";
							}
							result += "</select>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
									+ "<input type='button' value ='Apply' id ='buttonId' onclick=CaseSensitive('"
									+ column + "','" + tableName + "')></div></div>";
							result += "</div>";
							result += "</div>";

							JSONObject datatypeobj = new JSONObject();
							datatypeobj.put("VARCHAR2", "VARCHAR2");
							datatypeobj.put("NUMBER", "NUMBER");
							datatypeobj.put("CLOB", "CLOB");
							datatypeobj.put("BLOB", "BLOB");
							datatypeobj.put("DATE", "DATE");
							datatypeobj.put("RAW", "RAW");
							result += "<div id ='sqlDataTypeId' class='sqlDataTypeClass'>";
							result += "<div id ='OptionId' class='OptionIdClass'>";
							result += "<span class='visionVisualizeCardValuesSpanClass'>Data Type:</span>"
									+ "<span class='columnOptionSpan'><select id ='columnDatatypeId' class='columnDatatypeClass'>"
									+ "<option value= " + columnType + ">" + columnType + "</option>";
							for (Object key : datatypeobj.keySet()) {
								String keyStr = (String) key;
								Object keyvalue = datatypeobj.get(keyStr);
								result += "<option value= '" + keyvalue + "'>" + keyStr + "</option>";
							}
							result += "</select></span>";
							result += "</div>";
							result += "<div class='sizeOptionDivMain'><input type=\"number\"id=\"quantity\"name=\"quantity\" min=\"1\" max=\"400\'style =\" placeholder=\"Size\"></div>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
									+ "<input type='button' value ='Apply' id ='buttonId' onclick=changeDataType('"
									+ column + "','" + tableName + "')></div>" + "</div>";
							result += "</div>";

							result += "<div id ='AggregateResultId'  class='AggregateResultClass'></div>";
							result += "</div>";

						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getAggregateResult(HttpServletRequest request) {
		String result = "";
		try {
			String column = request.getParameter("column");
			String aggregateType = request.getParameter("aggregateType");
			String table = request.getParameter("table");
			String columnname = aggregateType + "(" + column + ")";
			String selectQuery = "SELECT " + columnname + " FROM " + table + " ";
			List totaldata = access.sqlqueryWithParams(selectQuery, Collections.EMPTY_MAP);
			if (aggregateType != null && aggregateType.equalsIgnoreCase("AVG")) {
				BigDecimal data = (BigDecimal) totaldata.get(0);
				DecimalFormat df_obj = new DecimalFormat("##.###");
				result = aggregateType + "=" + df_obj.format(data);
			} else {
				result = aggregateType + "=" + totaldata.get(0);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String createSuffixAndPriffix(HttpServletRequest request) {
		String result = "";
		String updateQuery = "";
		String checkedVal = request.getParameter("checkedVal");
		String sufixandPrifixVal = request.getParameter("sufixandPrifixVal");
		String table = request.getParameter("table");
		String column = request.getParameter("column");
		try {
			if (checkedVal != null && !"".equalsIgnoreCase(checkedVal) && checkedVal.equalsIgnoreCase("SUFFIX")) {
				updateQuery = "UPDATE " + table + " SET " + column + " =" + column + " ||'" + sufixandPrifixVal + "'";
			} else {
				updateQuery = "UPDATE " + table + " SET " + column + "= '" + sufixandPrifixVal + "'||" + column + "";
			}
			int updateCount = access.executeUpdateSQLNoAudit(updateQuery, Collections.EMPTY_MAP);
			if (updateCount > 0) {
				result = "successfully updated";
			} else {

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String updatePalatteColor(HttpServletRequest request) {
        String result = "";
        try {
            String colorstr = request.getParameter("colorArr");
            String chartId = request.getParameter("chartId");
            String dashBoard = request.getParameter("dashBoard");
            dashBoard = dashBoard.replace("&amp;", "&");
            HashMap updatemap = new HashMap();
            String updateQuery = "";
            if (chartId.startsWith("visionVisualize")) {
                updateQuery = "UPDATE O_RECORD_VISUALIZATION set VISUALIZE_CUST_COL8 =:VISUALIZE_CUST_COL8 where CHART_ID=:CHART_ID";

                updatemap.put("VISUALIZE_CUST_COL8", colorstr);
                updatemap.put("CHART_ID", chartId);
            } else {
                updateQuery = "UPDATE O_RECORD_VISUALIZATION set VISUALIZE_CUST_COL8 =:VISUALIZE_CUST_COL8 where DASHBORD_NAME=:DASHBORD_NAME";
                updatemap.put("VISUALIZE_CUST_COL8", colorstr);
                updatemap.put("DASHBORD_NAME", dashBoard);
            }
            System.out.println("updateQuery::" + updateQuery);
            int count = access.executeUpdateSQLNoAudit(updateQuery, updatemap);
            System.out.println("Updated successfully::" + count);
            result = "Updated successfully::" + count;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

	@Transactional
	public JSONObject getDataCorrelation(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			MultiValueMap inputMap = new LinkedMultiValueMap();
			String fileName = request.getParameter("fileName");
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String filePath = fileStoreHomedirectory + "TreeDMImport/" + userName;
			fileName = filePath + File.separator + fileName;
			File outputFile = new File(fileName);
			FileSystemResource fileData = new FileSystemResource(outputFile);
			inputMap.add("fileName", fileData);
			String dataCorrelaltionApiUrl = "http://apihub.pilogcloud.com:6658/data_correlation/";
			HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<MultiValueMap<String, Object>>(inputMap,
					headers);
			RestTemplate template = new RestTemplate();
			ResponseEntity<JSONObject> response = template.postForEntity(dataCorrelaltionApiUrl, entity,
					JSONObject.class);
			JSONObject apiDataObj = response.getBody();
			return apiDataObj;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Transactional
	public JSONObject getAutoSuggestedChartTypes(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String result = "<div id ='visionAutoSuggestedChartTypes' class='visionAutoSuggestedChartTypesClass'>"
					+ "<span class='visionAutoSuggestionChartTypesSpan'>Please select the ChartType</span>"
					+ "<div id='visionAutoSuggestionChartTypeId' class='visionAutoSuggestionChartTypeClass'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Pie_Chart_Inner_Icon.svg', 'pie')\" src='images/Pie.svg' class='visualDarkMode' title='Pie chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Bar_Chart_Inner_Icon.svg', 'bar')\" src='images/Bar.svg' class='visualDarkMode' title='Bar chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Donut_Chart_Inner_Icon.svg', 'donut')\"  src='images/Donut.svg' class='visualDarkMode' title='Donut chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Column_Chart_Inner_Icon.svg', 'column')\"  src='images/Column.svg' class='visualDarkMode' title='Column chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Line_Chart_Inner_Icon.svg', 'lines')\"  src='images/Line.svg' class='visualDarkMode' title='Line chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Scatter_Chart_Inner_Icon.svg', 'scatter')\"  src='images/Scatter.svg' class='visualDarkMode' title='Scatter chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Tree_Chart_Inner_Icon.svg', 'treemap')\"  src='images/Tree_Chart.svg' class='visualDarkMode' title='Tree chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Histogram_Chart_Inner_Icon.svg', 'column')\"  src='images/Histogram.svg' class='visualDarkMode' title='Histogram chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Guage_Chart_Inner_Icon.svg', 'indicator')\"  src='images/Guage.svg' class='visualDarkMode' title='Guage chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Funnel_Chart_Inner_Icon.svg', 'funnel')\"  src='images/Funnel.svg' class='visualDarkMode' title='Funnel chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Candlestick_Chart_Inner_Icon.svg', 'candlestick')\"  src='images/Candlestick.svg' class='visualDarkMode' title='Candlestick chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Waterfall_Chart_Inner_Icon.svg', 'waterfall')\"  src='images/Waterfall.svg' class='visualDarkMode' title='Waterfall chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Redar-Chart-Thin.svg', 'scatterpolar')\"  src='images/Redar-Chart.svg' class='visualDarkMode' title='Radar chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('HeatMap_Inner_Icon.svg', 'heatMap')\"  src='images/HeatMap.svg' class='visualDarkMode' title='Heat Map'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('DashBoardCard.svg', 'Card')\"  src='images/DashBoardCard.svg' class='visualDarkMode' title='DashBordCard chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Bar_Chart_Inner_Icon.svg', 'barRotation')\" src='images/Bar.svg' class='visualDarkMode' title='Bar Label Rotation chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Sunburst_Inner_Icon.svg', 'sunburst')\" src='images/Sunburst.svg' class='visualDarkMode' title='Sunburst chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('GeoChart_Inner_Icon.svg', 'geochart')\" src='images/GeoChart.svg' class='visualDarkMode' title='Geo chart'>"
					+ "<img onclick=\"getAutoSuggestedChartDiv('Bar_Chart_Inner_Icon.svg', 'BarAndLine')\" src='images/Bar_Chart_Inner_Icon.svg' class='visualDarkMode' title='Bar and Line chart'>"
					+ "</div>" + "</div>";
			resultObj.put("result", result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}

	public JSONObject fetchBoxPlotChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			JSONArray dataArr = new JSONArray();
			String whereCondQuery = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String comboColumns = request.getParameter("comboColumns");
			String filterColumns = request.getParameter("filterColumns");
			String aggregateColumns = request.getParameter("aggregateColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String radioButtons = request.getParameter("radioButtons");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray comboColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray colsArr = new JSONArray();
			JSONArray aggregateColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}
			if (radioButtons != null && !"".equalsIgnoreCase(radioButtons)) {
				chartObj.put("radioButtonStr", getradioButtonsStr(chartId, radioButtons));
			}
			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}
			if (comboColumns != null && !"".equalsIgnoreCase(comboColumns) && !"null".equalsIgnoreCase(comboColumns)) {
				comboColsArr = (JSONArray) JSONValue.parse(comboColumns);
			}
			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}
			if (aggregateColumns != null && !"".equalsIgnoreCase(aggregateColumns)
					&& !"null".equalsIgnoreCase(aggregateColumns)) {
				aggregateColsArr = (JSONArray) JSONValue.parse(aggregateColumns);
			}
			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {
							aggregateColsArr.add(aggColumnName);
						}
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (comboColsArr != null && !comboColsArr.isEmpty()) {
				for (int i = 0; i < comboColsArr.size(); i++) {
					JSONObject comboColObj = (JSONObject) comboColsArr.get(i);
					if (comboColObj != null && !comboColObj.isEmpty()) {
						String columnName = (String) comboColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							colsArr.add(columnName);
						}
					}
				}
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			if (colsArr != null && !colsArr.isEmpty() && colsArr.size() <= 3 && tablesArr != null
					&& !tablesArr.isEmpty()) {
				String tableName = (String) tablesArr.get(0);
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
				}
				String groupBy = "";
				JSONArray totalDataArr = new JSONArray();
				int durationVal = 0;
				if (colsArr != null && !colsArr.isEmpty() && colsArr.size() <= 3) {
					String xAxisColumn = (String) colsArr.get(0);
					String yAxisColumn = (String) colsArr.get(1);
					String dataColumn = (String) colsArr.get(2);
					if (!(aggregateColsArr != null && !aggregateColsArr.isEmpty())) {
						yAxisColumn = "SUM(" + yAxisColumn + ")";
					}
					groupBy = " GROUP BY " + xAxisColumn + " , " + dataColumn;
					String columnNames = xAxisColumn + "," + yAxisColumn + "," + dataColumn;
					String xAxisQuery = "SELECT " + columnNames + " FROM " + tableName + " " + whereCondQuery + groupBy
							+ " ORDER BY " + xAxisColumn + " ASC";
					List selectData = access.sqlqueryWithParams(xAxisQuery, new HashMap());
					if (selectData != null && !selectData.isEmpty()) {
						JSONArray headerArr = new JSONArray();
						headerArr.add("Name");
						headerArr.add("Achieved");
						headerArr.add("Duration");
						totalDataArr.add(headerArr);
						for (int i = 0; i < selectData.size(); i++) {
							Object[] objData = (Object[]) selectData.get(i);
							if (objData != null) {
								if (i == 0) {
									durationVal = ((BigDecimal) objData[2]).intValue();
								}
								JSONArray valArr = new JSONArray();
								valArr.add(objData[0]);
								valArr.add(objData[1]);
								valArr.add(objData[2]);
								totalDataArr.add(valArr);
							}
						}

					}
				}

				chartObj.put("data", totalDataArr);
				chartObj.put("durationVal", durationVal);
				chartObj.put("tableName", tableName);
				chartObj.put("dataPropObject", dataPropObj);
				chartObj.put("layout", layoutObj);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public JSONObject getDateColumns(HttpServletRequest request) {
		JSONObject dateObj = new JSONObject();
		Connection connection = null;
		try {
			String result = "";
			String tableName = request.getParameter("tableName");
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				result = "<div  class='visionVisualizeChartTableColDateClass'>";
				result = "<div  class='visionVisualizeChartTableToggleClass'>";
//                result = "<div id='" + tableName + "_ID' class='visionVisualizeChartTableToggleClass'>";
				result += "<div class='visionVisualizeChartTableClass'><img src=\"images/nextrightarrow.png\" id=\"VisionImageVisualizationTableId\" class=\"VisionImageVisualizationTableClass\" title=\"Show/Hide Table\"/>"
						+ "<img src=\"images/GridDB.png\" class=\"VisionImageVisualizationTableImageClass\" title=\"Show/Hide Table\"/><h6>"
						+ tableName + "</h6></div>";
				result += "<ul class='visionVisualizationDragColumns'>";
				result += "<div class='columnFilterDiv'><input type='text' id='name' class='columnFilterationClass' placeholder='Search Column'></div>";
				result += "<div class='tableColumnsList'>";
				JSONArray dataColsArr = new JSONArray();
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					String id = tableName + "_" + columnName;
					if (columnType != null && !"".equalsIgnoreCase(columnType) && "DATE".equalsIgnoreCase(columnType)) {
						dataColsArr.add(columnName);
						result += "<li id=\"" + id
								+ "\" ><img src=\"images/calendar.svg\" onclick=getDateColumnCalendar('" + tableName
								+ "','" + columnName + "') class=\"VisionImageVisualizationTableClass\"/>" + columnName
								+ "</li>";

					}
				}
				result += "<li style='display:none'><span>No Columns Found</span></li>";
				result += "</div></ul>";
				result += "</div>";
				result += "<div class='visionVisualizeChartTableColumnDateCalendarClass'>";
				for (int k = 0; k < dataColsArr.size(); k++) {
					String colName = (String) dataColsArr.get(k);
					result += "<div id='" + dataColsArr.get(k)
							+ "_calendar' style=\"background: #fff; cursor: pointer; padding: 5px 10px; border: 1px solid #ccc; width: 400px;display:none\"><i class=\"fa fa-calendar\"></i><span></span><i class=\"fa fa-caret-down\"></i></div>";
				}
				result += "</div>";
				result += "</div>";
				dateObj.put("result", result);
				dateObj.put("dataColsArr", dataColsArr);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return dateObj;
	}

	@Transactional
	public JSONObject getQueryGridData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONArray dataFieldsArray = new JSONArray();
		JSONArray columnsArray = new JSONArray();
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		List<String> columnsList = new ArrayList();
		int recordsCount = 0;
		try {
			JSONObject connectionObj = new PilogUtilities().getDatabaseDetails(dataBaseDriver, dbURL, userName,
					password, "Current_V10");
			Connection connectionObject = DriverManager.getConnection(dbURL, userName, password);
			if (connectionObject instanceof Connection) {
				connection = (Connection) connectionObject;
				String query = request.getParameter("query");
				String pagenum = request.getParameter("pagenum");
				String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize") : "10";
				String recordendindex = request.getParameter("recordendindex");
				String recordstartindex = (request.getParameter("recordstartindex"));
				String getOnlyDataArray = (request.getParameter("getOnlyDataArray"));
				int startIndex = 0;
				int endIndex = 0;
				if (recordstartindex != null && recordendindex != null && pagesize != null) {
					startIndex = Integer.parseInt(recordstartindex);
					endIndex = Integer.parseInt(recordendindex);
				}

				String conditionQuery = "";

				Integer filterscount = 0;
				String filterCondition = "";

				String orderby = "";
				String sortdatafield = request.getParameter("sortdatafield");
				System.out.println("sortdatafield::::" + sortdatafield);
				String sortorder = request.getParameter("sortorder");
				if (!(sortdatafield != null && !"".equalsIgnoreCase(sortdatafield))) {
					sortdatafield = (String) request.getAttribute("sortdatafield");
				}
				if (!(sortorder != null && !"".equalsIgnoreCase(sortorder))) {
					sortorder = (String) request.getAttribute("sortorder");
				}
				System.out.println("sortorder::::" + sortorder);
				if (sortdatafield != null && sortorder != null
						&& (sortorder.equals("asc") || sortorder.equals("desc"))) {
					orderby = " ORDER BY " + sortdatafield + " " + sortorder;
				}

				conditionQuery += orderby;
				if (connectionObj != null
						&& "ORACLE".equalsIgnoreCase(String.valueOf(connectionObj.get("CONN_CUST_COL1")))) {
					conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
				} else if (connectionObj != null
						&& "MYSQL".equalsIgnoreCase(String.valueOf(connectionObj.get("CONN_CUST_COL1")))) {
					conditionQuery += " LIMIT " + startIndex + "," + pagesize + "";
				} else if (connectionObj != null
						&& "MSSQL".equalsIgnoreCase(String.valueOf(connectionObj.get("CONN_CUST_COL1")))) {
					if (!(orderby != null && !"".equalsIgnoreCase(orderby) && !"null".equalsIgnoreCase(orderby))) {
						conditionQuery += " ORDER BY (SELECT NULL) ";
					}
					conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
				}
				if (query != null && !"".equalsIgnoreCase(query) && !"null".equalsIgnoreCase(query)) {

					String selectQuery = (String) query;
					String chartDataQuery = "";

					if (!"null".equalsIgnoreCase(selectQuery) && !"".equalsIgnoreCase(selectQuery)) {
						/*
						 * String[] chartDataQueryArr = selectQuery.split(";"); if
						 * (chartDataQueryArr.length > 1) { chartDataQuery = chartDataQueryArr[1]; }
						 */
						chartDataQuery = selectQuery;
					}

					if (chartDataQuery != null && !"".equalsIgnoreCase(chartDataQuery)
							&& !"null".equalsIgnoreCase(chartDataQuery)) {
						System.out.println("Tree Data query::" + chartDataQuery);
						preparedStatement = connection.prepareStatement(chartDataQuery);
						resultSet = preparedStatement.executeQuery();
						ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
						int columnCount = resultSetMetaData.getColumnCount();

						if (getOnlyDataArray != null && "Y".equalsIgnoreCase(getOnlyDataArray)) {

							while (resultSet.next()) {
								JSONObject dataObj = new JSONObject();

								for (int i = 1; i <= columnCount; i++) {
									JSONObject dataFieldsObj = new JSONObject();
									String columnType = resultSetMetaData.getColumnTypeName(i);
									String columnName = resultSetMetaData.getColumnName(i);
									Object data = null;
									if ("DATE".equalsIgnoreCase(columnType) || "DATETIME".equalsIgnoreCase(columnType)
											|| "TIMESTAMP".equalsIgnoreCase(columnType)) {
										data = resultSet.getString(columnName);
									} else {
										data = resultSet.getObject(columnName);
									}
									if (data instanceof byte[]) {
										byte[] bytesArray = (byte[]) data;

										data = new RAW(bytesArray).stringValue();
									}
									if (columnName.contains("(") && columnName.contains(")")) {
										columnName = columnName.substring(columnName.indexOf("(") + 1,
												columnName.indexOf(")"));
									}
									String columnLabelName = "";
									if (columnName.contains(".")) {
										columnName = columnName.split("\\.")[1];
									}
									dataObj.put(columnName, data);

								}
								// System.out.println("dataObj::" + dataObj.size());
								dataArray.add(dataObj);

							}
							if (recordsCount != 0) {
								dataArray.add(recordsCount);
							}

							resultObj.put("dataArray", dataArray);
						} else {

							for (int i = 1; i <= columnCount; i++) {
								JSONObject dataFieldsObj = new JSONObject();
								String columnType = resultSetMetaData.getColumnTypeName(i);
								String columnName = resultSetMetaData.getColumnName(i);
								String colLabel = (columnName).toLowerCase().replace("_", " ");
								colLabel = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
										.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
										.collect(Collectors.joining(" "));
								dataFieldsObj.put("name", columnName);
								dataFieldsObj.put("type", "string");

								dataFieldsArray.add(dataFieldsObj);

								JSONObject columnsObject = new JSONObject();

								columnsObject.put("text", colLabel);
								columnsObject.put("datafield", columnName);
								columnsObject.put("width", 120);
								columnsObject.put("sortable", true);
								columnsArray.add(columnsObject);
							}

							resultObj.put("dataFieldsArray", dataFieldsArray);
							resultObj.put("columnsArray", columnsArray);

						}
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		return resultObj;
	}

	public Object getConnection(JSONObject dbObj) {
		Connection connection = null;
		Object returnedObj = null;
		try {
			if (dbObj != null && !dbObj.isEmpty()) {
				String initParamClassName = "com.pilog.mdm.access.V10MigrationDataAccess";
				String initParamMethodName = "get" + dbObj.get("CONN_CUST_COL1") + "Connection";
				System.out.println(
						" initParamClassName:" + initParamClassName + "initParamMethodName:" + initParamMethodName);
				Class clazz = Class.forName(initParamClassName);
				Class<?>[] paramTypes = { String.class, String.class, String.class, String.class, String.class };
				Method method = clazz.getMethod(initParamMethodName.trim(), paramTypes);
				Object targetObj = new PilogUtilities().createObjectByClass(clazz);
				returnedObj = method.invoke(targetObj, String.valueOf(dbObj.get("HOST_NAME")),
						String.valueOf(dbObj.get("CONN_PORT")), String.valueOf(dbObj.get("CONN_USER_NAME")),
						String.valueOf(dbObj.get("CONN_PASSWORD")), String.valueOf(dbObj.get("CONN_DB_NAME")));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnedObj;
	}

	@Transactional
	public JSONObject viewAnalyticsTableDataGrid(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONArray dataFieldsArray = new JSONArray();
		JSONArray columnsArray = new JSONArray();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection connection = null;
		JSONArray checkBoxDataArr = new JSONArray();
		String buttonDiv = "";

		int recordsCount = 0;
		try {
			String tableName = request.getParameter("tableName");

			String groupscount = request.getParameter("groupscount");
			String pagenum = request.getParameter("pagenum");
			String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize") : "10";
			String recordendindex = request.getParameter("recordendindex");
			String recordstartindex = (request.getParameter("recordstartindex"));

			String getOnlyDataArray = (request.getParameter("getOnlyDataArray"));
			connection = DriverManager.getConnection(dbURL, userName, password);
			int startIndex = 0;
			int endIndex = 0;
			if (recordstartindex != null && recordendindex != null && pagesize != null) {
				startIndex = Integer.parseInt(recordstartindex);
				endIndex = Integer.parseInt(recordendindex);
			}

			String conditionQuery = "";

			Integer filterscount = 0;
			String filterCondition = "";
			String selectQuery = "SELECT * FROM " + tableName;

			if (request.getParameter("filterscount") != null) {
				filterscount = new Integer(request.getParameter("filterscount"));
				filterCondition = buildFilterCondition(filterscount, request, dataBaseDriver);
				if (filterCondition != null && !"".equalsIgnoreCase(filterCondition)
						&& !"null".equalsIgnoreCase(filterCondition)) {
					if (selectQuery.contains("WHERE")) {
						selectQuery += " AND " + filterCondition;
					} else {
						selectQuery += " WHERE " + filterCondition;
					}
				}

			}

			String countQuery = "SELECT count(*) FROM (" + selectQuery + " )";
			ResultSet countResultSet = connection.prepareStatement(countQuery).executeQuery();
			while (countResultSet.next()) {
				recordsCount = countResultSet.getInt(1);

			}

			String orderby = "";
			String sortdatafield = request.getParameter("sortdatafield");
			System.out.println("sortdatafield::::" + sortdatafield);
			String sortorder = request.getParameter("sortorder");
			if (!(sortdatafield != null && !"".equalsIgnoreCase(sortdatafield))) {
				sortdatafield = (String) request.getAttribute("sortdatafield");
			}
			if (!(sortorder != null && !"".equalsIgnoreCase(sortorder))) {
				sortorder = (String) request.getAttribute("sortorder");
			}
			System.out.println("sortorder::::" + sortorder);
			if (sortdatafield != null && sortorder != null && (sortorder.equals("asc") || sortorder.equals("desc"))) {
				orderby = " ORDER BY " + sortdatafield + " " + sortorder;
			}

			selectQuery += orderby;
			if (dataBaseDriver.toUpperCase().contains("ORACLE")) {
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			} else if (dataBaseDriver.toUpperCase().contains("MYSQL")) {
				conditionQuery += " LIMIT " + startIndex + "," + pagesize + "";
			} else if (dataBaseDriver.toUpperCase().contains("MSSQL")) {
				if (!(orderby != null && !"".equalsIgnoreCase(orderby) && !"null".equalsIgnoreCase(orderby))) {
					selectQuery += " ORDER BY (SELECT NULL) ";
				}
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			}

			selectQuery = selectQuery + conditionQuery;

			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				String ClolumnListStr = "";
				try {
					List ColumnList = tableColumnList(request, tableName);
					if (ColumnList != null && !ColumnList.isEmpty()) {
						for (int i = 0; i < ColumnList.size(); i++) {
							String ColumnName = (String) ColumnList.get(i);
							checkBoxDataArr.add(ColumnName);
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			System.out.println("Tree Data query::" + selectQuery);
			preparedStatement = connection.prepareStatement(selectQuery);
			resultSet = preparedStatement.executeQuery();
			ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
			int columnCount = resultSetMetaData.getColumnCount();

			if (getOnlyDataArray != null && "Y".equalsIgnoreCase(getOnlyDataArray)) {

				while (resultSet.next()) {
					JSONObject dataObj = new JSONObject();

					for (int i = 1; i <= columnCount; i++) {
						JSONObject dataFieldsObj = new JSONObject();
						String columnType = resultSetMetaData.getColumnTypeName(i);
						String columnName = resultSetMetaData.getColumnName(i);
						Object data = null;
						if ("DATE".equalsIgnoreCase(columnType) || "DATETIME".equalsIgnoreCase(columnType)
								|| "TIMESTAMP".equalsIgnoreCase(columnType)) {
							data = resultSet.getString(columnName);
						} else if (columnType != null && !"".equalsIgnoreCase(columnType)
								&& "CLOB".equalsIgnoreCase(columnType)) {
							String popUpInsertString = new PilogUtilities()
									.clobToString((Clob) resultSet.getClob(columnName));
							if (popUpInsertString != null && !"".equalsIgnoreCase(popUpInsertString)) {
								data = popUpInsertString;
							}
						} else {
							data = resultSet.getObject(columnName);
						}
						if (data instanceof byte[]) {
							byte[] bytesArray = (byte[]) data;
							data = new RAW(bytesArray).stringValue();
						}
						dataObj.put(columnName, data);

					}

					dataArray.add(dataObj);

				}
				if (recordsCount != 0) {
					dataArray.add(recordsCount);
				}

				resultObj.put("data", dataArray);
			} else {
				for (int i = 1; i <= columnCount; i++) {
					JSONObject dataFieldsObj = new JSONObject();
					String columnType = resultSetMetaData.getColumnTypeName(i);
					String columnName = resultSetMetaData.getColumnName(i);
					dataFieldsObj.put("name", columnName);
					dataFieldsObj.put("type", "string");

					dataFieldsArray.add(dataFieldsObj);

					JSONObject columnsObject = new JSONObject();
					String colLabel = (columnName).toLowerCase().replace("_", " ");
					colLabel = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
							.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
							.collect(Collectors.joining(" "));
					columnsObject.put("text", colLabel);
					columnsObject.put("datafield", columnName);
					columnsObject.put("width", 120);
					columnsObject.put("sortable", true);
					columnsArray.add(columnsObject);

				}

				resultObj.put("dataFieldsArray", dataFieldsArray);
				resultObj.put("columnsArray", columnsArray);

				if (resultObj != null && !resultObj.isEmpty()) {
					buttonDiv = "<span class='visionGridViewButtonsClass'><img src='images/Delete-Icon-03-01.png' title='Delete' onclick=deleteColumn("
							+ checkBoxDataArr + ",'" + tableName + "','Y') >";
					buttonDiv += "<img src='images/Data Merge-Icon-01.png' title='Merge Data' onclick= mergeColumntwthData('"
							+ tableName + "','Y') >";
					buttonDiv += "<img src='images/Data Transpose-Icon-01-01.png' title='Transpose Data' onclick= composeData(event,'"
							+ tableName + "','Y') >";
					buttonDiv += "<img src='images/Dimention-Transpsose-Icon-02-01.png' title='Dimension Transpose' onclick= DimensionTranspose(event,'"
							+ tableName + "','Y') >";
					buttonDiv += "<img src='images/Change Datatype-Icon-03-01.png' title='Table Edit' onclick= ChooseOptions(event,'"
							+ tableName + "')>";
					buttonDiv += "<img src='images/Export-Icon-03-01.png' title='Export'id ='ExportgridId' onclick= generateexcel(event,'"
							+ tableName + "','Y') style='width: 20px;'>";
					buttonDiv += "<img src='images/Chart Auto-Suggetion-Icon-03-01.png' title='AI Chart Suggestions' onclick= getModalFileColumns(event,'"
							+ tableName + "') style='width: 20px;'>";
					buttonDiv += "<img src='images/Pivot Descriptor-Icon-03-01.png' title='Pivot Table' onclick= getCrossTabData('"
							+ tableName + "') >";
					buttonDiv += "<img src='images/Pivot Descriptor-Icon-03-01.png' title='Insights View' onclick= getInsightsDataView('"
							+ tableName + "') >";
					/*
					 * buttonDiv +=
					 * "<img src='images/Pivot table-Icon-03-01.png' title='Pivot table' onclick= getPivotGridData('"
					 * + tableName + "') >"; String filePath = null; buttonDiv +=
					 * "<img src='images/Pivot table-Icon-03-01.png' title='Data correlation' onclick= getDataCorrelation('"
					 * + filePath + "') >";  
					 */
					buttonDiv += "</span>";    
				}
				resultObj.put("buttons", buttonDiv);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}

	@Transactional
	public JSONObject executeSQLQuery(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONObject dataTypeCount = new JSONObject();
		dataTypeCount.put("VARCHAR2",0);
		dataTypeCount.put("NUMBER",0);
		Connection conn = null;
		PreparedStatement sqlStmt = null;
		ResultSet sqlResultSet = null;
		try {
			String script = request.getParameter("script");
			String connName = request.getParameter("connectionName");
			String tableNameFromArg = request.getParameter("tableName");
			//String[] columnsListArray	= 	request.getParameterValues("columnsList");
			String columnsListStrFromQuery = request.getParameter("columnsList");
//			ObjectMapper objectMapper = new ObjectMapper();
//			String[] columnsListArray = objectMapper.readValue(columnsListStrFromQuery, String[].class);
		JSONArray columnsListArrayFromQuery = new JSONArray();
			if (columnsListStrFromQuery != null && !"".equalsIgnoreCase(columnsListStrFromQuery)) {
				columnsListArrayFromQuery = (JSONArray) JSONValue.parse(columnsListStrFromQuery);
			}
			if (script != null && !"".equalsIgnoreCase(script) && !"null".equalsIgnoreCase(script) && connName != null
					&& !"".equalsIgnoreCase(connName) && !"null".equalsIgnoreCase(connName)) {
				script = script.toUpperCase();
				if ("Current_V10".equalsIgnoreCase(connName)) {
					Class.forName(dataBaseDriver);
					conn = DriverManager.getConnection(dbURL, userName, password);
				}
				if (conn != null) {
					sqlStmt = conn.prepareStatement(script);
					script = script.trim();
					script = script.replaceAll("\t", " ");
					script = script.replaceAll("\n", " ");

					String opType = script.substring(0, script.indexOf(" "));
					if (opType != null && !"".equalsIgnoreCase(opType)) {
						opType = opType.toUpperCase();
					}
					if (!"SELECT".equalsIgnoreCase(opType)
							|| (opType != null && !"".equalsIgnoreCase(opType) && !opType.startsWith("SELECT"))) {

						int noSelectCount = sqlStmt.executeUpdate();
						String message = StringUtils.capitalize(opType.toLowerCase()) + "ed Successfully.";
						if ("UPDATE".equalsIgnoreCase(opType) || "DELETE".equalsIgnoreCase(opType)) {
							message = noSelectCount + " Row(s) has been " + message;
						}
						resultObj.put("message", message);
						resultObj.put("messageFlag", true);
					} else {
						List columnNames = new ArrayList();
						if(script !=null && !"".equalsIgnoreCase(script) && script.contains("DISTINCT"))
						{
							 columnNames = (List) Arrays
									.asList((script.substring(script.indexOf("DISTINCT") + 9, script.indexOf("FROM") - 1))
											.split(","));
						}else {
							 columnNames = (List) Arrays
									.asList((script.substring(script.indexOf("SELECT") + 7, script.indexOf("FROM") - 1))
											.split(","));
						}
						
						sqlResultSet = sqlStmt.executeQuery();
						ResultSetMetaData resultSetMetaData = sqlResultSet.getMetaData();
						int columnCount = resultSetMetaData.getColumnCount();
						for (int i = 1; i <= columnCount; i++) {
							String columnName = resultSetMetaData.getColumnName(i);
							System.out.println("Column Name: " + columnName);
						}
						if (sqlResultSet.next()) {
							List<String> columnList = new ArrayList<>();
							if (true) {
								JSONObject gridProperties = new JSONObject();
								JSONObject gridObject = new JSONObject();
								List gridDataFieldsList = new ArrayList();
								List gridColumnsList = new ArrayList();
								String table = "";
//

//								table =resultSetMetaData.getTableName(0);
//								String tableName = extractTableNameFromQuery(sqlQuery);
								if (script.contains("JOIN")) {
									resultObj.put("joinQueryFlag", "true");
									if (script.contains("WHERE")) {
										table = script.substring(script.indexOf("FROM") + 5,
												script.indexOf("WHERE") - 1);
									} else if (script.contains("GROUP BY")) {
										table = script.substring(script.indexOf("FROM") + 5,
												script.indexOf("GROUP BY") - 1);
									} else if (script.contains("ORDER BY")) {
										table = script.substring(script.indexOf("FROM") + 5,
												script.indexOf("ORDER BY") - 1);
									}
								} else if (script.contains("WHERE")) {
									table = script.substring(script.indexOf("FROM") + 5, script.indexOf("WHERE") - 1);
									if(table.contains("SELECT")){
											String pattern = "FROM\\s+(\\w+)";
									        java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
									        java.util.regex.Matcher m = r.matcher(table);

									        if (m.find()) {
									        	table = m.group(1);
									        }

									        table = table;
											
										} else if (table.contains(",")) {
										resultObj.put("joinQueryFlag", "true");
										if (script.contains("GROUP BY")) {
											table = script.substring(script.indexOf("FROM") + 5,
													script.indexOf("GROUP BY") - 1);
										} else if (script.contains("ORDER BY")) {
											table = script.substring(script.indexOf("FROM") + 5,
													script.indexOf("ORDER BY") - 1);
										} else {
											table = script.substring(script.indexOf("FROM") + 5, script.length() - 1);
										}

									} else {
										table = script.substring(script.indexOf("FROM") + 5).split(" ")[0];
									}
								} else {
									int fromIndex = script.lastIndexOf("FROM");

									if (fromIndex != -1) {
										// Extract the substring starting from the index after the last "FROM"
										String substring = script.substring(fromIndex + "FROM".length()).trim();

										// Extract the table name until the first space or the end of the string
										int endIndex = substring.indexOf(' ');
										if (endIndex == -1) {
											table =  substring;
										} else {
											table =  substring.substring(0, endIndex);
										}
									}
								}
 
								for (int i = 1; i <= columnCount; i++) {
									String columnName,columnLabel = "";
									if (columnNames != null && !columnNames.isEmpty() && columnNames.contains("*")) {           
										columnName = resultSetMetaData.getColumnName(i);
										columnLabel = resultSetMetaData.getColumnLabel(i);
									} else {
										columnName = resultSetMetaData.getColumnName(i);
										columnLabel = resultSetMetaData.getColumnLabel(i);
									}
 									String columnClassName = resultSetMetaData.getColumnClassName(i);
									String columnCatgName = resultSetMetaData.getCatalogName(i);
									String columnType = resultSetMetaData.getColumnTypeName(i);
									String tableName = resultSetMetaData.getSchemaName(i);
									dataTypeCount.put(columnType,  (int) dataTypeCount.getOrDefault(columnType, 0)+ 1);
									JSONObject dataFieldObj = new JSONObject(); 

									JSONObject columnsObj = new JSONObject();

									if ("DATE".equalsIgnoreCase(columnType) || "DATETIME".equalsIgnoreCase(columnType)
											|| "TIMESTAMP".equalsIgnoreCase(columnType)) {
										columnType = "date";// 15
									}
									columnList.add(columnName);
									String aliasColName = "";
									if (columnName != null && !"".equalsIgnoreCase(columnName)
											&& columnName.contains("AS")) {
										aliasColName = columnName.split("AS")[1];
									}
									if (columnName.contains("(") && columnName.contains(")")) {
										columnName = columnName.substring(columnName.indexOf("(") + 1,
												columnName.indexOf(")"));
									}

									String columnLabelName = "";
									String colName = "";
									if (columnName.contains(".")) {
										colName = columnName.split("\\.")[1];

									} else {
										colName = columnName;

									}

									String colLabel = "";
									if (aliasColName != null && !"".equalsIgnoreCase(aliasColName)) {
										aliasColName = aliasColName.trim();
										colLabel = (aliasColName).toLowerCase().replace("_", " ");
										dataFieldObj.put("name", aliasColName);
									} else {
										colLabel = (colName).toLowerCase().replace("_", " ");
										dataFieldObj.put("name", colName);
									}

									colLabel = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
											.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
											.collect(Collectors.joining(" "));
									dataFieldObj.put("type", columnType);// 15
									columnsObj.put("text", colLabel);// 3
									columnsObj.put("editable", false);
									columnsObj.put("datafield",
											(aliasColName != null && !"".equalsIgnoreCase(aliasColName)) ? aliasColName
													: colName);
									columnsObj.put("width", ("20" + "%"));// 7
									columnsObj.put("showfilterrow", true);// 7
									columnsObj.put("cellsalign", "left");// 15
									columnsObj.put("align", "center");// 15
//	                                columnsObj.put("enabletooltips", true);
									columnsObj.put("filterable", true);
									columnsObj.put("sortable", true);
									columnsObj.put("filtercondition", "contains");
									columnsObj.put("enabletooltips", true);
									gridDataFieldsList.add(dataFieldObj);
									gridColumnsList.add(columnsObj);
								}
								if(tableNameFromArg != null && !"".equalsIgnoreCase(tableNameFromArg))
								{
									table = tableNameFromArg;
								}
								gridObject.put("datafields", gridDataFieldsList);
								gridObject.put("columns", gridColumnsList);
								gridObject.put("gridProperties", gridProperties);
								gridObject.put("columnList", columnList);
								resultObj.put("gridObject", gridObject);
								resultObj.put("tableName", table);
								resultObj.put("dataTypeCount", dataTypeCount);
								resultObj.put("columnsListForComplexQueries",columnsListArrayFromQuery);
							}
							
							resultObj.put("message", "Data Selected Succesfully.");
							resultObj.put("messageFlag", true);
							resultObj.put("selectFlag", true);
						} else {
							resultObj.put("message", "No Row(s) selected.");
							resultObj.put("messageFlag", true);
						}

					}
				} else {
					resultObj.put("message", "Unable to Connection Obj");
					resultObj.put("messageFlag", false);
				}
			}
		} catch (StringIndexOutOfBoundsException e) {
			resultObj.put("message", "Query/Script not valid");
			resultObj.put("messageFlag", false);
		} catch (Exception e) {
			resultObj.put("message", e.getMessage());
			resultObj.put("messageFlag", false);
			e.printStackTrace();
		} finally {
			try {
				if (sqlResultSet != null) {
					sqlResultSet.close();
				}
				if (sqlStmt != null) {
					sqlStmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return resultObj;
	}

	public JSONObject getSuggestedChartTypesBasedonColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			int colSize = 0;
			String colLength = request.getParameter("colLength");  
			String columnsListStr = request.getParameter("columnsList");
			String tableName = request.getParameter("tableName");
			String joinQueryFlag = request.getParameter("joinQueryFlag");
			String script = request.getParameter("script");
 			String prependFlag = request.getParameter("prependFlag");
			String dataTypeCountStr = request.getParameter("dataTypeCountObj");
			String methodName = request.getParameter("methodName");
			String columnsListForComplexQueries = request.getParameter("columnsListForComplexQueries");
			JSONArray colArr = new JSONArray();
			JSONArray colArrForComplexQueries = new JSONArray();
			String colListStrForComplexQueries="";
			if (columnsListStr != null && !"".equalsIgnoreCase(columnsListStr)) {
				colArr = (JSONArray) JSONValue.parse(columnsListStr);
			}
			if (columnsListForComplexQueries != null && !"".equalsIgnoreCase(columnsListForComplexQueries)) {
				colArrForComplexQueries = (JSONArray) JSONValue.parse(columnsListForComplexQueries);
				colListStrForComplexQueries = JSONArray.toJSONString(colArrForComplexQueries);
				colListStrForComplexQueries =colListStrForComplexQueries.replaceAll("\"", "#").replaceAll(" ", ":");
				//colListStrForComplexQueries =colListStrForComplexQueries.replaceAll(" ", "$");
			}
			if (colLength != null && !"".equalsIgnoreCase(colLength)) {
				colSize = Integer.parseInt(colLength);
			}
			JSONObject dataTypesObj = new JSONObject();
			if(dataTypeCountStr !=null && !"".equalsIgnoreCase(dataTypeCountStr) && !"".equalsIgnoreCase(dataTypeCountStr))
			{
			 dataTypesObj =(JSONObject) JSONValue.parse(dataTypeCountStr);
			}
			long varCharCnt = 0;
			long numberCnt = 0;
			if(dataTypesObj !=null && !dataTypesObj.isEmpty())
			{
				varCharCnt = (long) dataTypesObj.get("VARCHAR2");
				numberCnt = (long) dataTypesObj.get("NUMBER");
			}
			String result = "<div id ='visionSuggestedChartTypes' class='visionSuggestedChartTypesClass'>"
					+ "<span class='visionSuggestionChartTypesSpan'>Please select the ChartType</span>"
					+ "<div id='visionSuggestionChartTypeId' class='visionSuggestionChartTypeClass row iconsRow'>";
			List colList = (List) colArr.stream().map(i -> ((String) i).replaceAll(" ", ":"))
					.collect(Collectors.toList());

			String colListStr = "";

			if (colList != null && !colList.isEmpty()) {
				colListStr = JSONArray.toJSONString(colList);
			}
			if(colListStrForComplexQueries ==null || "".equalsIgnoreCase(colListStrForComplexQueries) ) {
				colListStrForComplexQueries = colListStr;
			}
			if (colSize == 1) {
				result += "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','indicator','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Guage.svg' class='visualDarkMode' title='Guage chart'></div>";
			} else if (colSize <= 2) {
				if(varCharCnt == 1 && numberCnt == 1) {
					result += "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','pie','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "') src='images/Pie.svg' class='visualDarkMode' title='Pie chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','bar','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Bar.svg' class='visualDarkMode' title='Bar chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','donut','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Donut.svg' class='visualDarkMode' title='Donut chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','column','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Column.svg' class='visualDarkMode' title='Column chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','lines','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Line.svg' class='visualDarkMode' title='Line chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','scatter','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Scatter.svg' class='visualDarkMode' title='Scatter chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','histogram','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Histogram.svg' class='visualDarkMode' title='Histogram chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','funnel','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Funnel.svg' class='visualDarkMode' title='Funnel chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','waterfall','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Waterfall.svg' class='visualDarkMode' title='Waterfall chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','scatterpolar','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Redar-Chart.svg' class='visualDarkMode' title='Radar chart'></div>"
							
							/*
							 * +
							 * "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= getSuggestedChartBasedonCols('"
							 * + colListStr + "','barRotation','" + tableName + "','" + joinQueryFlag +
							 * "','" + script + "','" + prependFlag +
							 * "') src='images/Bar.svg' class='visualDarkMode' title='Bar Label Rotation chart'></div>"
							 */
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStrForComplexQueries + "','treemap','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "')  src='images/Tree_Chart.svg' class='visualDarkMode' title='Tree chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','BasicAreaChart','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "') src='images/BasicAreaChart.png' class='visualDarkMode' title='Basic Area chart'></div>"
							
							+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
							+ colListStr + "','AreaPiecesChart','" + tableName + "','" + joinQueryFlag + "','" + script
							+ "','" + prependFlag + "') src='images/AreaPiecesChart.png' class='visualDarkMode' title='Basic Area chart'></div>";

				}
				
				
			} else if (2 < colSize) {
				if(varCharCnt == 1 && numberCnt >= 1) {
				result +="<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','bar','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Bar.svg' class='visualDarkMode' title='Bar chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','column','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Column.svg' class='visualDarkMode' title='Column chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','lines','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Line.svg' class='visualDarkMode' title='Line chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','scatter','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Scatter.svg' class='visualDarkMode' title='Scatter chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','histogram','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Histogram.svg' class='visualDarkMode' title='Histogram chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','funnel','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Funnel.svg' class='visualDarkMode' title='Funnel chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','candlestick','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Candlestick.svg' class='visualDarkMode' title='Candlestick chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','waterfall','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Waterfall.svg' class='visualDarkMode' title='Waterfall chart'></div>"
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','scatterpolar','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Redar-Chart.svg' class='visualDarkMode' title='Radar chart'></div>"
						
//						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= getSuggestedChartBasedonCols('"
//						+ colListStr + "','barRotation','" + tableName + "','" + joinQueryFlag + "','" + script
//						+ "','" + prependFlag + "') src='images/Bar.svg' class='visualDarkMode' title='Bar Label Rotation chart'></div>"
						
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','StackedAreaChart','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "') src='images/StackedAreaChart.png' class='visualDarkMode' title='Stacked Area Chart'></div>"
						
						+ "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStr + "','GradStackAreaChart','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "') src='images/GradientStackedAreaChart.png' class='visualDarkMode' title='Gradient Stacked Area chart'></div>";
		
				
				}
			if(varCharCnt >= 1 && numberCnt == 1) {
			result+= "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStrForComplexQueries + "','treemap','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Tree_Chart.svg' class='visualDarkMode' title='Tree chart'></div>"
						
						+"<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStrForComplexQueries + "','sunburst','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/Sunburst_Inner_Icon.svg' class='visualDarkMode' title='SunBurst'></div>"
						
						+"<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStrForComplexQueries + "','sankey','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/sankey_chart.png' class='visualDarkMode' title='Sankey'></div>";
			}
			if(varCharCnt == 2 && numberCnt == 1) {
				result+= "<div class=\"col-lg-4 my-2 px-1 col-md-4 visualChartsByQueryClass\"><img onclick= "+methodName+"('"
						+ colListStrForComplexQueries + "','heatMap','" + tableName + "','" + joinQueryFlag + "','" + script
						+ "','" + prependFlag + "')  src='images/HeatMap_Inner_Icon.svg' class='visualDarkMode' title='Heat Map'></div>";
						
			}
			}
			

		

			result += "</div>" + "</div>";
			//result = result.replaceAll("'Q'", "''Q''");


			resultObj.put("result", result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}
	public JSONObject fetchSankeyChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			JSONArray dataArr = new JSONArray();
			String whereCondQuery = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String radioButtons = request.getParameter("radioButtons");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray axisTablesArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray axisColumnsArr = new JSONArray();
			JSONArray valueColumnsArr = new JSONArray();
			JSONObject aggregateColsObj = new JSONObject();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}
			if (radioButtons != null && !"".equalsIgnoreCase(radioButtons)) {
				chartObj.put("radioButtonStr", getradioButtonsStr(chartId, radioButtons));
			}
			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}

			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}

			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						String tableName = (String) axisColObj.get("tableName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							axisColumnsArr.add(columnName);
							axisTablesArr.add(tableName);
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {
							aggregateColsObj.put(columnName, aggColumnName);
						}
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							valueColumnsArr.add(columnName);
						}
					}
				}
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}
			JSONArray linksArr = new JSONArray();
			if (axisColumnsArr != null && !axisColumnsArr.isEmpty() && valueColumnsArr != null
					&& !valueColumnsArr.isEmpty()) {
				String groupBy = "";
				String tableName = "";
				String xAxisQuery = "";
				for (int c = 0; c < axisColumnsArr.size() - 1; c++) {
					String fromColumn = (String) axisColumnsArr.get(c);
					String toColumn = (String) axisColumnsArr.get(c + 1);
					String dataColumn = (String) valueColumnsArr.get(c);
					if (!(aggregateColsObj != null && !aggregateColsObj.isEmpty()
							&& aggregateColsObj.get(dataColumn) != null
							&& !"".equalsIgnoreCase(String.valueOf(aggregateColsObj.get(dataColumn))))) {
						dataColumn = "SUM(" + dataColumn + ")";
					} else if (aggregateColsObj != null && !aggregateColsObj.isEmpty()
							&& !(aggregateColsObj.get(dataColumn) != null
									&& !"".equalsIgnoreCase(String.valueOf(aggregateColsObj.get(dataColumn))))) {
						dataColumn = "SUM(" + dataColumn + ")";
					}
					if (c == 0) {
						tableName = (String) axisTablesArr.get(c);
						groupBy = " GROUP BY " + fromColumn + " , " + toColumn;
						String columnNames = fromColumn + "," + toColumn + "," + dataColumn;
						xAxisQuery = "SELECT " + columnNames + " FROM " + tableName + " " + whereCondQuery + groupBy
								+ " ORDER BY " + fromColumn + " ASC";
					} else {
						String fromTableName = (String) axisTablesArr.get(c);
						String toTableName = (String) axisTablesArr.get(c+1);
						if (fromTableName != null && !"".equalsIgnoreCase(fromTableName) && toTableName != null
								&& !"".equalsIgnoreCase(toTableName) && fromTableName.equalsIgnoreCase(toTableName)) {
							tableName = fromTableName;
							groupBy = " GROUP BY " + fromColumn + " , " + toColumn;
							String columnNames = fromColumn + "," + toColumn + "," + dataColumn;
							xAxisQuery = "SELECT " + columnNames + " FROM " + tableName + " " + whereCondQuery + groupBy
									+ " ORDER BY " + fromColumn + " ASC";
						} else {
							groupBy = " GROUP BY " + fromColumn + " , " + toColumn;
							String columnNames = fromColumn + "," + toColumn + "," + dataColumn;
							xAxisQuery = "SELECT " + columnNames + " FROM " + fromTableName + ", " + toTableName + " "
									+ whereCondQuery + groupBy + " ORDER BY " + fromColumn + " ASC";

						}
					}

					List selectData = access.sqlqueryWithParams(xAxisQuery, new HashMap());
					if (selectData != null && !selectData.isEmpty()) {
						for (int i = 0; i < selectData.size(); i++) {
							Object[] objData = (Object[]) selectData.get(i);
							if (objData != null) {
								JSONObject linkObj = new JSONObject();
								linkObj.put("source", objData[0]);
								linkObj.put("target", objData[1]);
								linkObj.put("value", objData[2]);
								linksArr.add(linkObj);
								JSONObject dataObj = new JSONObject();
								dataObj.put("name", objData[0]);
								JSONObject dataObj1 = new JSONObject();
								dataObj1.put("name", objData[1]);
								if (dataArr != null && !dataArr.isEmpty()) {
									if (!dataArr.contains(dataObj1)) {
										dataArr.add(dataObj1);
									}
									if (!dataArr.contains(dataObj)) {
										dataArr.add(dataObj);
									}
								} else {
									dataArr.add(dataObj);
									dataArr.add(dataObj1);
								}
							}
						}

					}

				}

			}
			chartObj.put("data", dataArr);
			chartObj.put("links", linksArr);
			chartObj.put("dataPropObject", dataPropObj);
			chartObj.put("layout", layoutObj);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public List getCodeAndCurrencyList(HttpServletRequest request) {
		List listOfCodeAndCurrency = null;
		try {
			String query = "SELECT CODE, CURRENCY,SYMBOL FROM B_CURRENCIES WHERE ACTIVE_FLAG = 'Y' ORDER BY CODE ASC";
			listOfCodeAndCurrency = access.sqlqueryWithParams(query, Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return listOfCodeAndCurrency;
	}

	public Double getCurrencyConversionRate(HttpServletRequest request) {
		Double conversionRateDouble = null;
		try {
			String fromCurrencyDropDownValue = request.getParameter("fromCurrencyDropDownValue");
			String toCurrencyDropDownValue = request.getParameter("toCurrencyDropDownValue");
			if (fromCurrencyDropDownValue != null && !"".equalsIgnoreCase(fromCurrencyDropDownValue)
					&& toCurrencyDropDownValue != null && !"".equalsIgnoreCase(toCurrencyDropDownValue)) {
				List<String> conversionRatioList = dashboardutils.getCurrencyConvertedData(request,
						fromCurrencyDropDownValue, toCurrencyDropDownValue);
				String conversionRatioStr = conversionRatioList.get(0);
				conversionRateDouble = Double.parseDouble(conversionRatioStr);
				System.out.println("From Currency :::" + fromCurrencyDropDownValue + "\n toCurrencyDropDownValue ::: "
						+ toCurrencyDropDownValue + "\n Conversion Rate ::: " + conversionRateDouble);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conversionRateDouble;
	}

	@Transactional
	public JSONObject getAutoSuggestedFilterTables(HttpServletRequest request) {
		JSONObject chartSlicerData = new JSONObject();
		Connection connection = null;
		try {
			String result = "";
			String tableName = request.getParameter("tableName");
			String resultStr = "<div id='HomeSlicerColumndataId' class = 'HomeSlicerColumndataClass'>"
					+ "<div id=\"VisualizeBIHomeSlicerColumns\"></div>"
					+ "<div id=\"visualizeChartHomeSlicerData\" class=\"visualizeChartHomeSlicerClass\"></div>"
					+ "</div>";

			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				Class.forName(dataBaseDriver);
				connection = DriverManager.getConnection(dbURL, userName, password);
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					result += "<div  class='visionVisualizeHomeChartTableToggleClass'>";
					result += "<div class='visionVisualizeChartTableClass'><img src=\"images/nextrightarrow.png\" id=\"VisionImageVisualizationTableId\" class=\"VisionImageVisualizationHomeTableClass\" title=\"Show/Hide Table\"/>"
							+ "<img src=\"images/GridDB.png\" class=\"VisionImageVisualizationTableImageClass\" title=\"Show/Hide Table\"/><h6>"
							+ tableName + "</h6></div>";
					result += "<ul class='visionVisualizationDragColumns'>";
					result += "<div class='homechartSlicerColumnsDiv'>";
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metadata.getColumnName(i);
						String columnType = metadata.getColumnTypeName(i);
						String id = tableName + "_" + columnName;
						if (columnType != null && !"".equalsIgnoreCase(columnType)
								&& "NUMBER".equalsIgnoreCase(columnType)) {
							result += "<li id=\"" + id
									+ "\" ><img src=\"images/sigma-icon.jpg\" class=\"VisionImageVisualizationHomeTableClass\"/>"
									+ columnName + "</li>";
						} else if (columnType != null && !"".equalsIgnoreCase(columnType)
								&& "DATE".equalsIgnoreCase(columnType)) {
							result += "<li id=\"" + id
									+ "\" ><img src=\"images/calendar.svg\" class=\"VisionImageVisualizationHomeTableClass\"/>"
									+ columnName + "</li>";
						} else {
							result += "<li id=\"" + id + "\" >" + columnName + "</li>";
						}
					}
					result += "</div></ul>";
					result += "</div>";
				}

				chartSlicerData.put("result", result);
				chartSlicerData.put("resultStr", resultStr);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartSlicerData;
	}

	@Transactional
	public JSONObject getArtificialIntellisenseApiDetails(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			HttpSession httpSession = request.getSession(false);
			String ssOrgnId = (String) httpSession.getAttribute("ssOrgId");
			String chartId = request.getParameter("chartId");
//			JSONObject labelsObj = cloudUtills.getMultilingualObject(request);
//
			String lovQuery = "SELECT COUNT(VISUALIZE_CUST_COL11) FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID";
			Map<String, Object> lovMap = new HashMap<>();
//			lovMap.put("DLOV_NAME", lovName);
			lovMap.put("CHART_ID", chartId);
			List lovValuesList = access.sqlqueryWithParams(lovQuery, lovMap);
			if (lovValuesList != null && !lovValuesList.isEmpty()) {
				int fetchCount = new PilogUtilities().convertIntoInteger(lovValuesList.get(0));
				if (fetchCount <= 0) {
					JSONObject dataArrayObj = getChartFilterObjectData(request);
					JSONObject resultObjs = getChartDataObjectList(request, dataArrayObj);
					List chartList = (List) resultObjs.get("chartList");
					List<String> columnKeys = (List) resultObjs.get("columnKeys");
					JSONObject emptyObj = new JSONObject();
					JSONObject layoutObj = new JSONObject();
					JSONObject mainObj = getFramedChartDataObject(chartList, columnKeys, emptyObj, layoutObj, "");
					JSONObject dataObj = (JSONObject) mainObj.get("dataObj");
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
					MultiValueMap<String, String> inputMap = new LinkedMultiValueMap<>();
					inputMap.add("CHART_ID", chartId);
					inputMap.add("data", dataObj.toString());
					HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(
							inputMap, headers);
					RestTemplate template = new RestTemplate();
					ResponseEntity<JSONObject> response = template
							.postForEntity("http://idxp.pilogcloud.com:6648/chart_insights/", entity, JSONObject.class);
					JSONObject apiDataObj = response.getBody();
//                System.out.println("response"+apiDataObj.toString());
					if (!(apiDataObj != null)) {
						String mainQuery = "SELECT VISUALIZE_CUST_COL11 FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID";
						Map<String, Object> mainMap = new HashMap<>();
						mainMap.put("CHART_ID", chartId);
						List levenValuesList = access.sqlqueryWithParams(mainQuery, mainMap);
						if (levenValuesList != null && !levenValuesList.isEmpty() && levenValuesList.size() > 0) {
							String result = (String) levenValuesList.get(0);
							resultObj.put("message", result);
							resultObj.put("flag", false);
						}
					}
				} else {
					String mainQuery = "SELECT VISUALIZE_CUST_COL11 FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID";
					Map<String, Object> mainMap = new HashMap<>();
					mainMap.put("CHART_ID", chartId);
					List levenValuesList = access.sqlqueryWithParams(mainQuery, mainMap);
					if (levenValuesList != null && !levenValuesList.isEmpty()) {
						String result = (String) levenValuesList.get(0);
						resultObj.put("message", result);
						resultObj.put("flag", true);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			resultObj.put("message", "No Data Available.");
			resultObj.put("flag", false);
		}
		return resultObj;
	}

	public JSONObject getChartDataObjectList(HttpServletRequest request, JSONObject resultObj) {
		JSONObject chartListObj = new JSONObject();
		try {
			JSONArray dataarr = (JSONArray) resultObj.get("dataarr");
			JSONObject dataObj = (JSONObject) dataarr.get(0);
			boolean flag = false;
			String selectQuery = "";
			String whereCondQuery = "";
			String groupByCond = "";
			String axisColumns = (String) dataObj.get("axisColumns");
			String valuesColumns = (String) dataObj.get("valuesColumns");
			String filterColumns = (String) dataObj.get("filterColumns");
			String chartPorpObj = (String) dataObj.get("chartPorpObj");
			String tables = (String) dataObj.get("tablesObj");
			String chartType = (String) dataObj.get("chartType");
			String JoinQuery = (String) dataObj.get("joinQuery");
			String selectedvalue = (String) dataObj.get("selectedValue");
			String Slicecolumn = (String) dataObj.get("SliceColumn");
			String dragtableName = (String) dataObj.get("dragtableName");
			String startIndex = (String) dataObj.get("startIndex");
			String endIndex = (String) dataObj.get("endIndex");
			String pageSize = (String) dataObj.get("pageSize");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray tablesArr = new JSONArray();
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns)) {
				JSONArray axisColumnsArr = (JSONArray) JSONValue.parse(axisColumns);
				for (int i = 0; i < axisColumnsArr.size(); i++) {
					JSONObject axisObj = (JSONObject) axisColumnsArr.get(i);
					if (axisObj != null && !axisObj.isEmpty()) {
						JSONObject axisColumnObj = new JSONObject();
						String tableName = (String) axisObj.get("tableName");
						axisColumnObj.put("tableName", axisObj.get("tableName"));
						axisColumnObj.put("columnName", axisObj.get("columnName"));
						axisColsArr.add(axisColumnObj);
						if (!(tablesArr.contains(tableName))) {
							tablesArr.add(tableName);
						}
					}
				}

			}

			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)) {
				JSONArray valuesColumnsArr = (JSONArray) JSONValue.parse(valuesColumns);
				for (int i = 0; i < valuesColumnsArr.size(); i++) {
					JSONObject valueObj = (JSONObject) valuesColumnsArr.get(i);
					if (valueObj != null && !valueObj.isEmpty()) {
						JSONObject valueColumnObj = new JSONObject();
						valueColumnObj.put("tableName", valueObj.get("tableName"));
						valueColumnObj.put("columnName", valueObj.get("columnName"));
						valueColumnObj.put("aggColumnName", valueObj.get("aggColumnName"));
						valuesColsArr.add(valueColumnObj);
						String tableName = (String) valueObj.get("tableName");
						if (!(tablesArr.contains(tableName))) {
							tablesArr.add(tableName);
						}
					}
				}
			}
			String orderBy = "";
			int limit = 0;
			int startLimit = 0;
			if (startIndex != null && !"".equalsIgnoreCase(startIndex) && endIndex != null
					&& !"".equalsIgnoreCase(endIndex)) {
				limit = (int) Integer.parseInt(endIndex);
				startLimit = Integer.parseInt(startIndex);
			}
			List<String> columnKeys = new ArrayList<>();

			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}

			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							String[] columns = columnName.split(",");
							if (columns != null && columns.length > 0) {
								for (int j = 0; j < columns.length; j++) {
									String column = columns[j];
									String[] filteredColumnnameArr = column.split("\\.");
									String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
									if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
											&& !"null".equalsIgnoreCase(filteredColumnname)) {
										filteredColumnname = filteredColumnname.replaceAll("_", " ");
									}
									columnKeys.add(filteredColumnname);
									selectQuery += " " + column + ", ";
									whereCondQuery += column + " IS NOT NULL ";
									groupByCond += column + ", ";
									if (i < axisColsArr.size() - 1) {
										whereCondQuery += " AND ";
									}

								}
							}
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						String[] filteredColumnnameArr = columnName.split("\\.");
						String filteredColumnname = filteredColumnnameArr[1].replaceAll("\\)", "");
						if (filteredColumnname != null && !"".equalsIgnoreCase(filteredColumnname)
								&& !"null".equalsIgnoreCase(filteredColumnname)) {
							filteredColumnname = filteredColumnname.replaceAll("_", " ");
						}
						columnKeys.add(filteredColumnname + "ASCOL" + i);
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {
							selectQuery += " " + columnName + " AS COL" + i + " ,";
							if (i == 0) {
								orderBy += " ORDER BY COL" + i + " DESC ";
							}
							flag = true;
						} else {
							selectQuery += " " + columnName + ", ";
							if (i == 0) {
								orderBy += " ORDER BY " + columnName + " DESC ";
							}
							groupByCond += columnName;
							if (i < valuesColsArr.size() - 1) {
								groupByCond += ",";
							}
						}
					}
				}

				if (chartType != null && !"".equalsIgnoreCase(chartType)
						&& ("indicator".equalsIgnoreCase(chartType) || "Card".equalsIgnoreCase(chartType))) {
					groupByCond = "";
				} else if (!flag) {
					groupByCond = "";
				} else if (groupByCond != null && !"".equalsIgnoreCase(groupByCond)) {
					groupByCond = new PilogUtilities().trimChar(groupByCond, ',');
					groupByCond = " GROUP BY " + groupByCond;
				}

			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						if (i == 0 && whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)) {
							whereCondQuery += " AND ";
						}
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}

			if (selectQuery != null && !"".equalsIgnoreCase(selectQuery) && tablesArr != null && !tablesArr.isEmpty()) {

				String tableName = (String) tablesArr.get(0);
				String countQuery = "";
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
					tableName = JoinQuery;
					selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
					selectQuery = "SELECT " + selectQuery + " " + tableName + whereCondQuery + groupByCond + orderBy;
					countQuery = "SELECT COUNT(*) " + tableName + whereCondQuery + groupByCond;
					System.out.println("selectQuery :::" + selectQuery);
				} else {
					selectQuery = new PilogUtilities().trimChar(selectQuery, ',');
					selectQuery = "SELECT " + selectQuery + " FROM " + tableName + whereCondQuery + groupByCond
							+ orderBy;
					countQuery = "SELECT COUNT(*) FROM " + tableName + whereCondQuery + groupByCond;
					System.out.println("selectQuery :::" + selectQuery);
				}

				if (countQuery != null && !"".equalsIgnoreCase(countQuery)) {
					List countData = access.sqlqueryWithParams(countQuery, new HashMap());
					if (countData != null && !countData.isEmpty()) {
						chartListObj.put("totalChartCount", countData.get(0));
					}
				}

			}
			List selectData = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), limit, 0);
			if (selectData != null && !selectData.isEmpty()) {
				chartListObj.put("chartList", selectData);
			}
			if (columnKeys != null && !columnKeys.isEmpty()) {
				chartListObj.put("columnKeys", columnKeys);
			}
			System.out.println("selectQuery :::" + selectQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chartListObj;
	}

	@Transactional
	public JSONObject getChartFilterObjectData(HttpServletRequest request) {
		JSONObject tabledataobj = new JSONObject();
		JSONArray dataarr = new JSONArray();
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String chartId = (String) request.getParameter("chartId");
			String updatechartdata = (String) request.getParameter("filterConditions");
			String selectquery = "SELECT " + "X_AXIS_VALUE, "// 0
					+ "Y_AXIS_VALUE,"// 1
					+ "CHART_TYPE,"// 2
					+ "TABLE_NAME,"// 3
					+ "CHART_ID,"// 4
					+ "AGGRIGATE_COLUMNS, "// 5
					+ "FILTER_CONDITION, "// 6
					+ "CHART_PROPERTIES, "// 7
					+ "CHART_CONFIG_OBJECT, "// 8
					+ "VISUALIZE_CUST_COL10, "// 9
					+ "CHART_TITTLE, " // 10
					+ "VISUALIZE_CUST_COL8, " // 11
					+ "VISUALIZE_CUST_COL9, " // 12
					+ "VISUALIZE_CUST_COL5, " // 13
					+ "FILTER_COLUMN, " // 14
					+ "VISUALIZE_CUST_COL6 " // 15
					+ "FROM " + "O_RECORD_VISUALIZATION " + "WHERE " + "CHART_ID =:CHART_ID "
					// + "AND CHART_TYPE =:CHART_TYPE "
					+ "ORDER BY CHART_SEQUENCE_NO";
			HashMap datamap = new HashMap();
			datamap.put("CHART_ID", chartId);
			List datalist = access.sqlqueryWithParams(selectquery, datamap);
			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object[] rowData = (Object[]) datalist.get(i);
					JSONObject dataobj = new JSONObject();
					dataobj.put("axisColumns", rowData[0]);
					dataobj.put("valuesColumns", rowData[1]);
					dataobj.put("type", rowData[2]);
					dataobj.put("table", rowData[3]);
					dataobj.put("chartid", rowData[4]);
					dataobj.put("aggColumnName", rowData[5]);
					dataobj.put("filterColumns", rowData[6]);
					dataobj.put("chartPropObj", rowData[7]);
					dataobj.put("chartConfigObj", rowData[8]);
					dataobj.put("labelLegend", rowData[9]);
					dataobj.put("Lebel", rowData[10]);
					dataobj.put("colorsObj", rowData[11]);
					dataobj.put("chartConfigToggleStatus", rowData[12]);
					dataobj.put("compareChartsFlag", rowData[13]);
					dataobj.put("homeFilterColumn", rowData[14]);
					dataobj.put("fetchQuery", rowData[15]);
					dataarr.add(dataobj);
				}
				tabledataobj.put("dataarr", dataarr);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return tabledataobj;
	}

	@Transactional
	public JSONObject getFramedChartDataObject(List selectData, List<String> columnKeys, JSONObject layoutObj,
			JSONObject dataPropObj, String chartType) {

		JSONArray colorsArr = new JSONArray();
		JSONArray markerColorsArr = new JSONArray();
		JSONObject dataObj = new JSONObject();
		JSONObject framedChartDataObj = new JSONObject();
		if (dataPropObj != null && !dataPropObj.isEmpty()) {
			JSONObject markerObj = (JSONObject) dataPropObj.get("marker");
			if (markerObj != null && !markerObj.isEmpty()) {
				if (markerObj.get("colors") instanceof JSONArray) {
					colorsArr = (JSONArray) markerObj.get("colors");
				} else {
					String colorValues = (String) markerObj.get("colors");
					if (colorValues != null && !"".equalsIgnoreCase(colorValues)
							&& !"null".equalsIgnoreCase(colorValues)) {
						colorsArr.add(colorValues);
					}
				}

			}
		}
		if (selectData != null && !selectData.isEmpty()) {
			int c = 0;
			if (chartType != null && !"".equalsIgnoreCase(chartType) && "indicator".equalsIgnoreCase(chartType)) {
				long indicatorVal = 0;
				for (int i = 0; i < selectData.size(); i++) {
					if (selectData.get(i) instanceof String) {
						String rowData = (String) selectData.get(i);
						if (rowData != null && !"".equalsIgnoreCase(rowData)) {
							indicatorVal += Integer.parseInt(rowData);
						}
					} else if (selectData.get(i) instanceof BigDecimal) {
						BigDecimal rowData = (BigDecimal) selectData.get(i);
						if (rowData != null) {
							indicatorVal = rowData.longValue();
						}
					}

					if (colorsArr != null && !colorsArr.isEmpty()) {
						if (c > colorsArr.size() - 1) {
							c = 0;
						}
						markerColorsArr.add(colorsArr.get(c));
					}
					c++;
				}
				dataObj.put(columnKeys.get(0), indicatorVal);

			} else {
				for (int i = 0; i < selectData.size(); i++) {
					Object[] rowData = (Object[]) selectData.get(i);
					for (int j = 0; j < rowData.length; j++) {
						if (dataObj != null && !dataObj.isEmpty() && dataObj.get(columnKeys.get(j)) != null) {
							JSONArray jsonDataArr = (JSONArray) dataObj.get(columnKeys.get(j));
							if (rowData[j] != null) {
								jsonDataArr.add(rowData[j]);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							} else {
								jsonDataArr.add(0);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							}
						} else {
							JSONArray jsonDataArr = new JSONArray();
							if (rowData[j] != null) {
								jsonDataArr.add(rowData[j]);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							} else {
								jsonDataArr.add(0);
								dataObj.put(columnKeys.get(j), jsonDataArr);
							}
						}
					}

					if (colorsArr != null && !colorsArr.isEmpty()) {
						if (c > colorsArr.size() - 1) {
							c = 0;
						}
						markerColorsArr.add(colorsArr.get(c));
					}
					c++;
				}
			}
			framedChartDataObj.put("dataObj", dataObj);
		}

		if (layoutObj != null && !layoutObj.isEmpty()) {
			JSONObject markerObj = (JSONObject) layoutObj.get("marker");
			if (markerObj != null && !markerObj.isEmpty() && markerColorsArr != null && !markerColorsArr.isEmpty()) {
				markerObj.put("colors", markerColorsArr);
			}
			framedChartDataObj.put("layoutObj", layoutObj);
		}

		return framedChartDataObj;
	}

	public JSONObject alterBiTable(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		String message = "";
		ResultSet columns = null;
		ResultSet pkColumns = null;
		List pkColumnsList = new ArrayList();
		List dataTypesList = new ArrayList();
		DatabaseMetaData metaData = null;
		JSONArray columnsArray = new JSONArray();
		JSONArray dataFieldsArray = new JSONArray();
		JSONArray data = new JSONArray();
		PreparedStatement preparedStatement = null;

		try {
			dataTypesList.add("VARCHAR2");
			dataTypesList.add("CHAR");
			dataTypesList.add("NUMBER");
			dataTypesList.add("INTEGER");
			dataTypesList.add("DATE");
			dataTypesList.add("LONG");
			dataTypesList.add("CLOB");
			dataTypesList.add("BLOB");
			dataTypesList.add("NVARCHAR2");
			dataTypesList.add("NCHAR");
			dataTypesList.add("DECIMAL");
			dataTypesList.add("VARCHAR2");
			String tableName = request.getParameter("tableName");
			resultObj.put("dataTypesList", dataTypesList);
			connection = DriverManager.getConnection(dbURL, userName, password);
			metaData = connection.getMetaData();
			pkColumns = metaData.getPrimaryKeys("DH101102", null, tableName);

			while (pkColumns.next()) {
				String columnName = pkColumns.getString("COLUMN_NAME");
				pkColumnsList.add(columnName);
			}
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				for (int i = 1; i <= columnCount; i++) {
					JSONObject row = new JSONObject();
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					int nullable = metadata.isNullable(i);
					int size = metadata.getColumnDisplaySize(i);
					row.put("columnName", columnName);
					row.put("datatypeName", columnType);
					if (pkColumnsList.contains(columnName)) {
						row.put("primaryKey", "Y");
					} else {
						row.put("primaryKey", "N");
					}
					if (columnType != null && ("NUMBER".equalsIgnoreCase(columnType)
							|| "NUMERIC".equalsIgnoreCase(columnType) || "DECIMAL".equalsIgnoreCase(columnType)
							|| "FLOAT".equalsIgnoreCase(columnType))) {
						row.put("columnsize", "");
						row.put("precision", size);
					} else {
						row.put("columnsize", size);
						row.put("precision", "");
						row.put("scale", "");
					}
					if (nullable == 0) {
						row.put("notNull", "NOT NULL");
					} else {
						row.put("notNull", "NULL");
					}
					data.add(row);
				}
			}

			String pkColsListStr = (String) pkColumnsList.stream().map(e -> e).collect(Collectors.joining(","));
			resultObj.put("pkColsList", pkColsListStr);
			resultObj.put("data", data);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				System.out.println("connection closed :: " + connection.isClosed());
			} catch (Exception e) {

			}
		}

		return resultObj;
	}

	@Transactional
	public String gettableformStr(HttpServletRequest request) {
		Connection connection = null;
		String result = "";
		try {
			String tableName = request.getParameter("table");
			String column = request.getParameter("column");
			String value = request.getParameter("value");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				connection = dashboardutils.getCurrentConnection();
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metadata.getColumnName(i);
						String columnType = metadata.getColumnTypeName(i);
						if (column != null && column.equalsIgnoreCase(columnName)) {

							String gridId = request.getParameter("gridId");

							result = "<div id ='AggregateBiColumnmainId'  class='AggregateBiColumnmainClass'>";
														result += "<div class='ColumnRenameClass' ><span class='title'>RenameColumn: </span><span class='inputField'><input type='text' style='text-transform:uppercase' id='ColumnRenameid'/></span></div>";
														result += "<div id='applybuttonId' class='applybuttonClass'>"
																+ "<input type='button' value ='Apply' id ='buttonId' onclick=renametableColumn('"
																+ column + "','" + tableName +"','" + gridId + "')></div>";
														result += "</div>";

						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getSelectType(HttpServletRequest request) {
		Connection connection = null;
		String result = "";
		try {
			String tableName = request.getParameter("table");
			String column = request.getParameter("column");
			String value = request.getParameter("value");
			String gridId = request.getParameter("gridId");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				connection = dashboardutils.getCurrentConnection();
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metadata.getColumnName(i);
						String columnType = metadata.getColumnTypeName(i);
						if (column != null && column.equalsIgnoreCase(columnName)) {

							result += "<div id ='AggregateBiColumnId'  class='AggregateBiColumnClass'>";
							result += "<span class='title'>Select Type :</span><span class='inputField'><select id ='smartBiSelect' class='smartBiSelectClass'"
									+ "onchange=getAggregateResult('" + column + "','" + tableName + "','" + gridId +"')></span>";
							if (columnType != null && columnType.equalsIgnoreCase("VARCHAR2")) {
								result += "<option value='SELECT'>Select</option>"
										+ "<option value='COUNT'>COUNT</option>";
							} else if (columnType != null && columnType.equalsIgnoreCase("NUMBER")) {
								result += "<option value='SELECT'>Select</option>"
										+ "<option value='COUNT'>Count</option>" + "<option value='SUM'>Sum</option>"
										+ "<option value='AVG'>Average</option>"
										+ "<option value='MIN'>Minimum</option>"
										+ "<option value='MAX'>Maximum</option>"
										+ "<option value='MEDIAN'>Median</option>";
							}
							result += "</select>";
							result += "</span></div>";
							result += "<div id ='AggregateResultId'  class='AggregateResultClass'></div>";
							result += "</div>";

						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getSuffixValue(HttpServletRequest request) {
		Connection connection = null;
		String result = "";
		try {
			String tableName = request.getParameter("table");
			String column = request.getParameter("column");
			String value = request.getParameter("value");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				connection = dashboardutils.getCurrentConnection();
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metadata.getColumnName(i);
						String columnType = metadata.getColumnTypeName(i);
						if (column != null && column.equalsIgnoreCase(columnName)) {
							String gridId = request.getParameter("gridId");
							result += "<div id ='sufixId' class='sufixClass'>";
							result += "<div class='ColumnRenameClass' ><span class='title'>Suffix: </span><span class='inputFeild'><input type='text' id='suffixId'/></span>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
									+ "<input type='button' value ='Apply' id ='buttonId' onclick=createSuffixAndPrifix('"
									+ column + "','" + tableName + "','" + gridId + "')></div></div>";
							result += "</div>";

						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getPrefixValue(HttpServletRequest request) {
		Connection connection = null;
		String result = "";
		try {
			String tableName = request.getParameter("table");
			String column = request.getParameter("column");
			String value = request.getParameter("value");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				connection = dashboardutils.getCurrentConnection();
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metadata.getColumnName(i);
						String columnType = metadata.getColumnTypeName(i);
						if (column != null && column.equalsIgnoreCase(columnName)) {
							String gridId = request.getParameter("gridId");

							result += "<div id ='prefixId' class='prifixClass'>";
														result += "<div class='ColumnRenameClass' ><span class='title'>Prefix: </span><span class='inputFeild'><input type='text'  id='prifixId' value=''/></span>";
														result += "<div id='applybuttonId' class='applybuttonClass'>"
																+ "<input type='button' value ='OK' id ='buttonId' onclick=createPrifix('" 
																+ column + "','" + tableName + "','" + gridId + "')></div></div>";
														result += "</div>";						}

					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String getCreateFind(HttpServletRequest request) {
		Connection connection = null;
		String result = "";
		try {
			String tableName = request.getParameter("table");
			String column = request.getParameter("column");
			String value = request.getParameter("value");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				connection = dashboardutils.getCurrentConnection();
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metadata.getColumnName(i);
						String columnType = metadata.getColumnTypeName(i);
						String gridId = request.getParameter("gridId");
						if (column != null && column.equalsIgnoreCase(columnName)) {
							result += "<div id='findValueId' class='actualValueClass'>";
							result += "<div class='ColumnRenameClass'><span class='title'>FindValue: </span><span class='inputFeild'><input type='text' id='actualValueId' value=''/></span>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
							        + "<input type='button' value='SearchAll' id='buttonId' onclick=searchWord('" + column + "','" + tableName + "','" + gridId + "')>"
							        + "<input type='button' value='FindNext' id='buttonId' onclick=searchNextWord('" + column + "','" + tableName + "','" + gridId + "')>"
							        + "</div></div>";
							result += "</div>";

							result += "<div id='renameValueDivId' class='renameValueClass'>";
							result += "<div class='ColumnRenameClass'><span class='title'>ReplaceValue: </span><span class='inputFeild'><input type='text' id='renameValueId' value=''/></span>";
							result += "<div id='applybuttonId' class='applybuttonClass'>"
							        + "<input type='button' value='ReplaceAll' id='buttonId' onclick=createRenameValue('" + column + "','" + tableName + "','" + gridId + "')>"
							        + "</div></div>";
							result += "<div class='caseSensitiveClass'><span class='title'>Case Sensitive </span><input type='checkbox' id='caseSensitiveCheckbox'></div>";
							
							result += "</div>";

						}



					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String getRenameValue(HttpServletRequest request) {
		String result = "";
		try {
			String findValue = request.getParameter("findValue");
			String renameValue = request.getParameter("renameValue");
			String column = request.getParameter("column");
			String table = request.getParameter("table");
			String updateValueQuery = "UPDATE " + table + " set " + column + "='" + renameValue + "' where " + column
					+ "='" + findValue + "'";
			int count = access.executeUpdateSQL(updateValueQuery, Collections.EMPTY_MAP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public JSONObject executeAlterTable(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		String message = "";
		try {
			String tableName = request.getParameter("tableName");
			connection = DriverManager.getConnection(dbURL, userName, password);
			String alterTableDataStr = request.getParameter("alterTableData");
			JSONObject alterTableData = (JSONObject) JSONValue.parse(alterTableDataStr);
			for (Object col : alterTableData.keySet()) {
				JSONObject alterColsObj = (JSONObject) alterTableData.get(col);
				String alterTableQuery = "";
				String dataTypeWithSize = "";
				if (alterColsObj.keySet().contains("datatypeName")) {
					dataTypeWithSize += alterColsObj.get("datatypeName");
					if (alterColsObj.keySet().contains("columnsize") && alterColsObj.get("columnsize") != null
							&& !"".equalsIgnoreCase((String) alterColsObj.get("columnsize"))) {
						dataTypeWithSize += "(" + alterColsObj.get("columnsize") + ")";
					} else if (alterColsObj.keySet().contains("precision") && alterColsObj.get("precision") != null
							&& !"".equalsIgnoreCase((String) alterColsObj.get("precision"))) {
						dataTypeWithSize += "(" + alterColsObj.get("precision");
						if (alterColsObj.keySet().contains("scale") && alterColsObj.get("scale") != null
								&& !"".equalsIgnoreCase((String) alterColsObj.get("scale"))) {
							dataTypeWithSize += "," + alterColsObj.get("scale");
						}
						dataTypeWithSize += ")";
					}
				}
				String defaultValStr = "";
				if (alterColsObj.keySet().contains("defaultValue")) {
					String defaultValue = (alterColsObj.get("defaultValue") != null
							&& !"".equalsIgnoreCase((String) alterColsObj.get("defaultValue")))
									? ((String) alterColsObj.get("defaultValue"))
									: "NULL";
					defaultValStr = " DEFAULT " + defaultValue;
				}
				String notNull = "";
				if (alterColsObj.keySet().contains("notNull") && alterColsObj.get("notNull") != null
						&& !"".equalsIgnoreCase((String) alterColsObj.get("notNull"))) {
					notNull = " " + alterColsObj.get("notNull");
				}

				if (alterColsObj.keySet().contains("columnName") && alterColsObj.get("columnName") != null
						&& !"".equalsIgnoreCase((String) alterColsObj.get("columnName"))
						&& ((String) col).startsWith("newRow_")) {

					String addAltTblQuery = "ALTER TABLE " + tableName + " ADD (" + alterColsObj.get("columnName") + " "
							+ dataTypeWithSize + " " + defaultValStr + " " + notNull + " )";
					try {
						Boolean tableAltered = executeAlterSQLQuery(request, connection, addAltTblQuery);
						message += "Column Added SuceesfUlly <br>";
					} catch (Exception e) {
						message += "Error  Adding column " + e.getMessage() + "<br>";
						e.printStackTrace();
					}
				} else if (!alterColsObj.keySet().contains("columnName")) {

					String modifyAltTblQuery = "ALTER TABLE " + tableName + " MODIFY (" + col + " " + dataTypeWithSize
							+ " " + defaultValStr + " " + notNull + " )";
					try {
						Boolean tableAltered = executeAlterSQLQuery(request, connection, modifyAltTblQuery);
						message += "Table Alterd SuceesfUlly <br>";
					} catch (Exception e) {
						message += "Error  Alter table " + e.getMessage() + "<br>";
						e.printStackTrace();
					}

				}

			}

			List finalPKColsList = new ArrayList();
			List existingPKcolsList = new ArrayList();
			String existingPKcols = request.getParameter("existingPKcols");
			if (existingPKcols != null && !"".equalsIgnoreCase(existingPKcols)) {
				existingPKcolsList = Arrays.asList(existingPKcols.split(","));
				finalPKColsList.addAll(existingPKcolsList);
			}
			List addPrimaryKeyColumnsList = (List) alterTableData.keySet().stream().filter(col -> {
				Boolean addPkFlag = false;
				JSONObject dataFieldsObj = (JSONObject) alterTableData.get(col);
				if (dataFieldsObj.keySet().contains("primaryKey")) {
					String currentVal = (String) dataFieldsObj.get("primaryKey");

					if ("Y".equalsIgnoreCase(currentVal)) {
						addPkFlag = true;
					} else {
						addPkFlag = false;
					}
				}
				return addPkFlag;

			}).map(col -> {
				if (((String) col).startsWith("newRow_")) {
					JSONObject dataFieldsObj = (JSONObject) alterTableData.get(col);
					if (dataFieldsObj.keySet().contains("columnName")) {
						col = (String) dataFieldsObj.get("columnName");
					}
				}
				finalPKColsList.add(col);
				return col;
			}).collect(Collectors.toList());

			List dropPrimaryKeyColumnsList = (List) alterTableData.keySet().stream().filter(col -> {
				Boolean dropPkFlag = false;
				JSONObject dataFieldsObj = (JSONObject) alterTableData.get(col);
				if (!((String) col).startsWith("newRow_") && dataFieldsObj.keySet().contains("primaryKey")) {
					String currentVal = (String) dataFieldsObj.get("primaryKey");
					if ("N".equalsIgnoreCase(currentVal)) {
						dropPkFlag = true;
					} else {
						dropPkFlag = false;
					}
				}

				return dropPkFlag;

			}).map(e -> {
				finalPKColsList.remove(e);
				return e;
			}).collect(Collectors.toList());

			try {
				if ((addPrimaryKeyColumnsList != null && !addPrimaryKeyColumnsList.isEmpty())
						|| (dropPrimaryKeyColumnsList != null && !dropPrimaryKeyColumnsList.isEmpty())) {

					try {
						String alterTblDropPKQuery = "ALTER TABLE " + tableName + " DROP  PRIMARY KEY";
						Boolean tableAltered = executeAlterSQLQuery(request, connection, alterTblDropPKQuery);

//                        message += "Table Alterd SuceesfUlly <br>";
					} catch (Exception e) {
//                        message += "Error  Alter table " + e.getMessage() + "<br>";
						e.printStackTrace();
					}

					String addPrimaryKeyColsStr = (String) finalPKColsList.stream().map(e -> e)
							.collect(Collectors.joining(","));

					String alterTblAddPKQuery = "ALTER TABLE " + tableName + " ADD  PRIMARY KEY ( "
							+ addPrimaryKeyColsStr + " )";
					Boolean tableAltered = executeAlterSQLQuery(request, connection, alterTblAddPKQuery);
					message += "Table Alterd SuceesfUlly <br>";
				}
			} catch (Exception e) {
				message += "Error  Alter table " + e.getMessage() + "<br>";
				String alterTblAddExistingPKQuery = "ALTER TABLE " + tableName + " ADD  PRIMARY KEY ( " + existingPKcols
						+ " )";
				Boolean tableAltered = executeAlterSQLQuery(request, connection, alterTblAddExistingPKQuery);
				e.printStackTrace();
			}

			for (Object col : alterTableData.keySet()) {
				if (!((String) col).startsWith("newRow_")) {
					JSONObject alterColsObj = (JSONObject) alterTableData.get(col);

					if (alterColsObj.keySet().contains("columnName")) {
						String renameColVal = (String) alterColsObj.get("columnName");
						String alterTableRenameQuery = "ALTER TABLE " + tableName + " RENAME COLUMN " + col + " TO "
								+ renameColVal;
						try {
							Boolean tableAltered = executeAlterSQLQuery(request, connection, alterTableRenameQuery);
							message += "Table Alterd SuceesfUlly <br>";
						} catch (Exception e) {
							message += "Error  Alter table " + e.getMessage() + "<br>";
							e.printStackTrace();
						}
					}
				}

			}

		} catch (Exception e) {
			message += "Error  Alter table " + e.getMessage() + "<br>";
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
				System.out.println("connection closed :: " + connection.isClosed());
			} catch (Exception e) {

			}
		}
		resultObj.put("message", message);
		return resultObj;
	}

	@Transactional
	public Boolean executeAlterSQLQuery(HttpServletRequest request, Connection connection, String query)
			throws SQLException {
		Boolean tableAltered = null;
		PreparedStatement preparedStatement = null;
		try {
			System.out.println(" query ::: " + query);
			preparedStatement = connection.prepareStatement(query);
			tableAltered = preparedStatement.execute();
		} finally {
			try {
				if (preparedStatement != null) {
					preparedStatement.close();
				}

			} catch (Exception e) {
			}
		}
		return tableAltered;
	}

	@Transactional
	public String createPrefixValue(HttpServletRequest request) {
		String result = "";
		String updateQuery = "";
		String checkedVal = request.getParameter("checkedVal");
		String sufixandPrifixVal = request.getParameter("sufixandPrifixVal");
		String PrifixVal = request.getParameter("PrifixVal");
		String table = request.getParameter("table");
		String column = request.getParameter("column");
		try {
			if (checkedVal != null && !"".equalsIgnoreCase(checkedVal) && checkedVal.equalsIgnoreCase("PRIFIX")) {

				updateQuery = "UPDATE " + table + " SET " + column + "= '" + PrifixVal + "'||" + column + "";
			}
			int updateCount = access.executeUpdateSQLNoAudit(updateQuery, Collections.EMPTY_MAP);
			if (updateCount > 0) {

				result = "successfully updated";
			} else {

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Transactional
	public String deleterowdata(HttpServletRequest request) {
		String rsult = "";
		try {
			String selectedrowdata = request.getParameter("selectedRowsData");
			String tablename = request.getParameter("tablename");
			JSONArray totalData = (JSONArray) JSONValue.parse(selectedrowdata);
			int msgcount = 0;
			for (int i = 0; i < totalData.size(); i++) {
				JSONObject rowdata = (JSONObject) totalData.get(i);
				String auditid = (String) rowdata.get("AUDIT_ID");
				if (auditid != null && !"".equalsIgnoreCase(auditid) && tablename != null
						&& !"".equalsIgnoreCase(tablename)) {
					String deleteQuery = "DELETE FROM " + tablename + " WHERE  AUDIT_ID = :AUDIT_ID";
					HashMap deletemap = new HashMap<>();
					deletemap.put("AUDIT_ID", auditid);
					int deletecount = access.executeUpdateSQLNoAudit(deleteQuery, deletemap);
					if (deletecount > 0) {
						msgcount++;
					}
				}
			}
			if (msgcount > 0) {
				rsult = "(" + msgcount + ")" + "Rows Delated Successfully";
			} else {
				rsult = "Failed to delete As there no primary Key (Audit ID)";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rsult;
	}

	@Transactional
	public JSONObject removeDuplicateValue(HttpServletRequest request) {
		String result = "";
		Connection connection = null;
		String tableName = request.getParameter("table");
		JSONArray checkBoxDataArr = new JSONArray();
		JSONObject resultobj = new JSONObject();
		try {
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					checkBoxDataArr.add(columnName);
				}

			}

			// resultobj.put("result", result);
			resultobj.put("checkBoxDataArr", checkBoxDataArr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public JSONObject removeDuplicateEachColumn(HttpServletRequest request) {
		String result = "";
		String table = request.getParameter("table");
		String column = request.getParameter("column");
		JSONObject resultobj = new JSONObject();
		int datacount = 0;
		try {
			String selectQuery = "SELECT  " + column + " FROM " + table + " GROUP BY " + column
					+ " HAVING COUNT(*) > 1";
			List datalist = access.sqlqueryWithParams(selectQuery, Collections.EMPTY_MAP);
			if (datalist != null && !datalist.isEmpty()) {
				if (datalist.size() > 0) {
					datacount++;
				}
			}
			if (datalist.size() > 0) {
				resultobj.put("result", datalist.size()
						+ " duplicate value(s) found! <br> Do you want to delete these duplicates??</br>");
			} else {
				resultobj.put("result", "No duplicates are found!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public JSONObject deleteDuplicateValues(HttpServletRequest request) {
		String result = "";
		String table = request.getParameter("table");
		String column = request.getParameter("column");
		JSONObject resultobj = new JSONObject();

		int deleteCount = 0;

		try {
			String selectQuery = "SELECT " + column + " FROM " + table + " GROUP BY " + column + " HAVING COUNT(*) > 1";
			List datalist = access.sqlqueryWithParams(selectQuery, Collections.EMPTY_MAP);

			if (datalist != null && !datalist.isEmpty()) {
				for (int i = 0; i < datalist.size(); i++) {
					Object value = datalist.get(i);
					String auditIdQuery = "SELECT AUDIT_ID FROM " + table + " WHERE " + column + "=:COL_VAL";
					Map auditMap = new HashMap();
					auditMap.put("COL_VAL", value);
					List auditDataList = access.sqlqueryWithParams(auditIdQuery, auditMap);
					for (int j = 1; j < auditDataList.size(); j++) {

						String auditId = String.valueOf(auditDataList.get(j));
						String deleteQuery = "DELETE FROM " + table + " WHERE AUDIT_ID=:AUDIT_ID";
						Map deleteMap = new HashMap();
						deleteMap.put("AUDIT_ID", auditId);
						deleteCount += access.executeUpdateSQLNoAudit(deleteQuery, deleteMap);
					}
				}
			}

			resultobj.put("result", deleteCount + " Rows Delete successfully!");
		} catch (Exception e) {
			resultobj.put("result",  "Delete Unsuccessfully! as there is no AUDIT_ID");
			e.printStackTrace();
		}
		return resultobj;
	}

	@Transactional
	public JSONObject executePythonQuery(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection conn = null;
		PreparedStatement sqlStmt = null;
		ResultSet sqlResultSet = null;
		try {
			String script = request.getParameter("script");
			String connName = request.getParameter("connectionName");
			if (script != null && !"".equalsIgnoreCase(script) && !"null".equalsIgnoreCase(script) && connName != null
					&& !"".equalsIgnoreCase(connName) && !"null".equalsIgnoreCase(connName)) {
				script = script.trim();

//				  script = script.replaceAll("\t", " "); 
//				  script = script.replaceAll("\n", " ");

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				MultiValueMap<String, String> inputMap = new LinkedMultiValueMap<>();
				inputMap.add("code", script);
				inputMap.add("flag", "Y");
				HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(
						inputMap, headers);
				RestTemplate template = new RestTemplate();
				ResponseEntity<JSONObject> response = template
						.postForEntity("http://idxp.pilogcloud.com:6648/run_script/", entity, JSONObject.class);
				JSONObject apiDataObj = response.getBody();

				if (apiDataObj != null && !apiDataObj.isEmpty()) {
					Map columns = (LinkedHashMap) apiDataObj.get("columns");
					if (columns != null && !columns.isEmpty()) {
						List<String> columnList = new ArrayList<>();
						JSONObject gridProperties = new JSONObject();
						JSONObject gridObject = new JSONObject();
						List gridDataFieldsList = new ArrayList();
						List gridColumnsList = new ArrayList();
						for (Object key : columns.keySet()) {
							String columnName = (String) key;
							String columnType = (String) columns.get(key);
							String tableName = "";
							JSONObject dataFieldObj = new JSONObject();

							JSONObject columnsObj = new JSONObject();

							if ("DATE".equalsIgnoreCase(columnType) || "DATETIME".equalsIgnoreCase(columnType)
									|| "TIMESTAMP".equalsIgnoreCase(columnType)) {
								columnType = "date";
							}
							if ("VARCHAR2".equalsIgnoreCase(columnType)) {
								columnType = "string";
							}
							columnList.add(columnName);
							String columnLabelName = "";
							String colName = "";
							if (columnName.contains(".")) {
								colName = columnName.split("\\.")[1];
							} else {
								colName = columnName;

							}

							dataFieldObj.put("name", colName);
							String colLabel = (colName).toLowerCase().replace("_", " ");
							colLabel = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
									.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
									.collect(Collectors.joining(" "));
							dataFieldObj.put("type", columnType);// 15
							columnsObj.put("text", colLabel);// 3
							columnsObj.put("editable", false);
							columnsObj.put("datafield", colName);
							columnsObj.put("width", ("20" + "%"));// 7
							columnsObj.put("showfilterrow", true);// 7
							columnsObj.put("cellsalign", "left");// 15
							columnsObj.put("align", "center");// 15
//                                columnsObj.put("enabletooltips", true);
							columnsObj.put("filterable", true);
							columnsObj.put("sortable", true);
							columnsObj.put("filtercondition", "contains");
							columnsObj.put("enabletooltips", true);
							gridDataFieldsList.add(dataFieldObj);
							gridColumnsList.add(columnsObj);
						}
						gridObject.put("datafields", gridDataFieldsList);
						gridObject.put("columns", gridColumnsList);
						gridObject.put("gridProperties", gridProperties);
						gridObject.put("columnList", columnList);
						resultObj.put("gridObject", gridObject);

						resultObj.put("message", "Data Selected Succesfully.");
						resultObj.put("messageFlag", true);
						resultObj.put("selectFlag", true);
					} else {
						resultObj.put("message", "No Row(s) selected.");
						resultObj.put("messageFlag", true);
					}

				} else {
					resultObj.put("message", "No Row(s) selected.");
					resultObj.put("messageFlag", true);
				}

			} else {
				resultObj.put("message", "Unable to Connection Obj");
				resultObj.put("messageFlag", false);
			}
		} catch (StringIndexOutOfBoundsException e) {
			resultObj.put("message", "Query/Script not valid");
			resultObj.put("messageFlag", false);
		} catch (Exception e) {
			resultObj.put("message", e.getMessage());
			resultObj.put("messageFlag", false);
			e.printStackTrace();
		} finally {
			try {
				if (sqlResultSet != null) {
					sqlResultSet.close();
				}
				if (sqlStmt != null) {
					sqlStmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
			}
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getPythonChartObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection conn = null;
		PreparedStatement sqlStmt = null;
		ResultSet sqlResultSet = null;
		try {
			String script = request.getParameter("query");
			String connName = request.getParameter("connectionName");
			if (script != null && !"".equalsIgnoreCase(script) && !"null".equalsIgnoreCase(script)) {
				script = script.trim();
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
				MultiValueMap<String, String> inputMap = new LinkedMultiValueMap<>();
				inputMap.add("code", script);
				inputMap.add("flag", "N");
				HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(
						inputMap, headers);
				RestTemplate template = new RestTemplate();
				ResponseEntity<JSONObject> response = template
						.postForEntity("http://idxp.pilogcloud.com:6648/run_script/", entity, JSONObject.class);
				JSONObject apiDataObj = response.getBody();
				JSONArray dataArray = new JSONArray();
				if (apiDataObj != null && !apiDataObj.isEmpty()) {
					List data = (ArrayList) apiDataObj.get("data");
					List columns = (ArrayList) apiDataObj.get("columns");
					if (data != null && !data.isEmpty() && columns != null && !columns.isEmpty()) {
						int columnsCount = columns.size();
						int recordsCount = data.size();
						for (int i = 0; i < data.size(); i++) {
							JSONObject dataObj = new JSONObject();
							List dataRowArr = (ArrayList) data.get(i);
							for (int j = 0; j < columnsCount; j++) {
								dataObj.put(columns.get(j), dataRowArr.get(j));
							}
							dataArray.add(dataObj);
						}
						if (recordsCount != 0) {
							dataArray.add(recordsCount);
						}

						resultObj.put("dataArray", dataArray);

					} else {
						resultObj.put("message", "No Data to Display.");
						resultObj.put("messageFlag", true);
					}

				} else {
					resultObj.put("message", "No Data to Display.");
					resultObj.put("messageFlag", true);
				}

			} else {
				resultObj.put("message", "Unable to Process Script");
				resultObj.put("messageFlag", false);
			}
		} catch (StringIndexOutOfBoundsException e) {
			resultObj.put("message", "Query/Script not valid");
			resultObj.put("messageFlag", false);
		} catch (Exception e) {
			resultObj.put("message", e.getMessage());
			resultObj.put("messageFlag", false);
			e.printStackTrace();
		} finally {
			try {
				if (sqlResultSet != null) {
					sqlResultSet.close();
				}
				if (sqlStmt != null) {
					sqlStmt.close();
				}
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
			}
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getCardDateValues(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		try {
			HttpSession httpSession = request.getSession(false);
			String ssOrgnId = (String) httpSession.getAttribute("ssOrgId");
			String columnName = request.getParameter("columnName");
			String tableName = request.getParameter("tableName");
			String count = request.getParameter("count");
			String cardDateDiv = "";
			JSONArray dataColArr = new JSONArray();
			if (tableName != null && !"".equalsIgnoreCase(tableName) && columnName != null
					&& !"".equalsIgnoreCase(columnName)) {
				connection = dashboardutils.getCurrentConnection();
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					for (int i = 1; i <= columnCount; i++) {
						String column = metadata.getColumnName(i);
						if (column != null && !"".equalsIgnoreCase(column) && column.equalsIgnoreCase(columnName)) {
							String columnType = metadata.getColumnTypeName(i);
							String id = tableName + "." + column;
							if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "VARCHAR2".equalsIgnoreCase(columnType)) {
								String lovQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName + " WHERE "
										+ columnName + " IS NOT NULL";
								Map<String, Object> lovMap = new HashMap<>();
								List lovValuesList = access.sqlqueryWithParams(lovQuery, lovMap);
								String optionStr = "";
								if (lovValuesList != null && !lovValuesList.isEmpty()) {
									for (int j = 0; j < lovValuesList.size(); j++) {
										String optionVal = (String) lovValuesList.get(j);
										optionStr += "<option value='" + optionVal + "'>" + optionVal + "</option>";
									}

								}
								cardDateDiv += "<div class='innerFilterDivClass innerFilterDivStrClass'><div id ='visionVisualizeCardDateValueId"
										+ count + "' class='visionVisualizeCardDateValueClass homepageCardDateSelect'>"
										+ "<div class='innerFilterDivStrFromClass homepageCardEditInnerFrom'>"
										+ "<span>From :</span>" + "<div id='visionVisualizeCardDateFromValueId" + count
										+ "' class='visionVisualizeCardDateFromValueClass'>"
										+ "<select id='visionVisualizeCardDateFromSelectValueId" + count
										+ "' class='visionVisualizeCardDateFromSelectValueClass' onchange='getCardDateFromSelectValue()'>"
										+ optionStr + "</select>" + "</div>" + "</div>"
										+ "<div class='innerFilterDivStrToClass homepageCardEditInnerTo'>"
										+ "<span>To :</span>" + "<div id='visionVisualizeCardDateToValueId" + count
										+ "' class='visionVisualizeCardDateToValueClass'>"
										+ "<select id='visionVisualizeCardDateToSelectValueId" + count
										+ "' class='visionVisualizeCardDateToSelectValueClass' onchange='getCardDateToSelectValue()'>"
										+ optionStr + "</select>" + "</div>" + "</div>" + "</div></div>";
								resultObj.put("cardDateDiv", cardDateDiv);
							} else if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "DATE".equalsIgnoreCase(columnType)) {
								String fromOperatorId = getOperators("From", count);
								cardDateDiv += "<div class='innerFilterDivClass innerFilterDivDateClass'><div class='feildsOperatorDiv'><span class='visionVisualizeCardDateFromColLabelClass'>"
										+ "From Operator: </span>" + "<span id='tdOperatorsFrom" + count
										+ "' class='visionVisualizeCardFromColOperatorsClass'>" + fromOperatorId
										+ "</span></div>" + "<div class='visionVisualizeCardFromColValuesClass'>"
										+ "<span><input autocomplete='off' placeholder='From' id='tbminFrom" + count
										+ "'"
										+ " value='' class='paramtd_range jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic' type='text'></span>"
										+ "<span style='display:none;' id='toFrom" + count
										+ "'>To </span><span><input id='tbmaxFrom" + count
										+ "' autocomplete='off' placeholder='To' value=''"
										+ " class='paramtd_range jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic' type='text'></span>"
										+ "</div></div>";
								String toOperatorId = getOperators("To", count);
								cardDateDiv += "<div class='innerFilterDivClass innerFilterDivDateClass'><div class='feildsOperatorDiv'><span class='visionVisualizeCardDateToColLabelClass'>"
										+ "To Operator: </span>" + "<span id='tdOperatorsTo" + count
										+ "' class='visionVisualizeCardToColOperatorsClass'>" + toOperatorId
										+ "</span></div>" + "<div class='visionVisualizeCardToColValuesClass'>"
										+ "<span><input autocomplete='off' id='tbminTo" + count + "'"
										+ " value='' placeholder='From' class='paramtd_range jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic' type='text'></span>"
										+ "<span style='display:none;' id='toTo" + count
										+ "'>To </span><span><input id='tbmaxTo" + count
										+ "' autocomplete='off' placeholder='To' value=''"
										+ " class='paramtd_range jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic' type='text'></span>"
										+ "</div></div>";

								JSONObject dateFromMin = new JSONObject();
								dateFromMin.put("tbid", "tbminFrom" + count);
								dateFromMin.put("type", "min");
								dataColArr.add(dateFromMin);
								JSONObject dateFromMax = new JSONObject();
								dateFromMax.put("tbid", "tbmaxFrom" + count);
								dateFromMax.put("type", "max");
								dataColArr.add(dateFromMax);
								JSONObject dateToMin = new JSONObject();
								dateToMin.put("tbid", "tbminTo" + count);
								dateToMin.put("type", "min");
								dataColArr.add(dateToMin);
								JSONObject dateToMax = new JSONObject();
								dateToMax.put("tbid", "tbmaxTo" + count);
								dateToMax.put("type", "max");
								dataColArr.add(dateToMax);
								resultObj.put("dataColArr", dataColArr);
								resultObj.put("cardDateDiv", cardDateDiv);

							}
						}

					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			resultObj.put("message", "No Data Available.");
		}
		return resultObj;
	}

	public String getOperators(String type, String count) {
		String result = "";
		try {
			JSONObject operators = new JSONObject();
			operators.put("BETWEEN", "Between");
			operators.put("EQUALS", "=");
			operators.put("NOT EQUALS", "<>");
			operators.put("GREATER THAN", ">");
			operators.put("LESS THAN", "<");
			operators.put("IN", "In");
			operators.put("NOT IN", "Not In");
			result += "<span class='visionVisualizeCardDateOperatorClass'>";
			result += "<select id='ddw" + type + count + "' class='visionVisualizeCardDateSelectOperatorClass'>";
			for (Object key : operators.keySet()) {
				String keyName = (String) key;
				String value = (String) operators.get(keyName);
				result += "<option value='" + keyName + "'>" + value + "</option>";
			}
			result += "</select>";
			result += "</span>";
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	public JSONObject fetchHorizontalBarChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			JSONObject finalDataObj = new JSONObject();
			JSONArray yAxisArr = new JSONArray();
			String whereCondQuery = "";
			String chartId = request.getParameter("chartId");
			String axisColumns = request.getParameter("axisColumns");
			String valuesColumns = request.getParameter("valuesColumns");
			String filterColumns = request.getParameter("filterColumns");
			String tables = request.getParameter("tablesObj");
			String chartType = request.getParameter("chartType");
			String JoinQuery = request.getParameter("joinQuery");
			String selectedvalue = request.getParameter("selectedValue");
			String Slicecolumn = request.getParameter("SliceColumn");
			String dragtableName = request.getParameter("dragtableName");
			String radioButtons = request.getParameter("radioButtons");
			JSONArray axisColsArr = new JSONArray();
			JSONArray valuesColsArr = new JSONArray();
			JSONArray filterColsArr = new JSONArray();
			JSONArray axisColumnsArr = new JSONArray();
			JSONArray valueColumnsArr = new JSONArray();
			JSONObject aggregateColsObj = new JSONObject();
			JSONArray tablesArr = new JSONArray();
			JSONObject filteredChartConfigObj = new JSONObject();
			JSONObject chartConfigObj = new JSONObject();
			String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}
			if (radioButtons != null && !"".equalsIgnoreCase(radioButtons)) {
				chartObj.put("radioButtonStr", getradioButtonsStr(chartId, radioButtons));
			}
			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
					chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
				axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
			}
			if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
					&& !"null".equalsIgnoreCase(valuesColumns)) {
				valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
			}

			if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
					&& !"null".equalsIgnoreCase(filterColumns)) {
				filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
			}

			if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
				tablesArr = (JSONArray) JSONValue.parse(tables);
			}
			if (axisColsArr != null && !axisColsArr.isEmpty()) {
				for (int i = 0; i < axisColsArr.size(); i++) {
					JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
					if (axisColObj != null && !axisColObj.isEmpty()) {
						String columnName = (String) axisColObj.get("columnName");
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							axisColumnsArr.add(columnName);
						}
					}
				}
			}

			if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
				for (int i = 0; i < valuesColsArr.size(); i++) {
					JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
					if (valueColObj != null && !valueColObj.isEmpty()) {
						String columnName = (String) valueColObj.get("columnName");
						String aggColumnName = (String) valueColObj.get("aggColumnName");
						if (aggColumnName != null && !"".equalsIgnoreCase(aggColumnName)
								&& !"".equalsIgnoreCase(aggColumnName)) {
							aggregateColsObj.put(columnName, aggColumnName);
						}
						if (columnName != null && !"".equalsIgnoreCase(columnName)) {
							valueColumnsArr.add(columnName);
						}
					}
				}
			}

			if (filterColsArr != null && !filterColsArr.isEmpty()) {
				for (int i = 0; i < filterColsArr.size(); i++) {
					JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
					if (filterColObj != null && !filterColObj.isEmpty()) {
						whereCondQuery += buildCondition(filterColObj, request);
						if (i != filterColsArr.size() - 1) {
							whereCondQuery += " AND ";
						}
					}
				}
			}
			if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
					&& !"".equalsIgnoreCase(Slicecolumn)) {
				whereCondQuery += dragtableName + "." + Slicecolumn + " ";
				whereCondQuery += "IN";
				String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
				whereCondQuery += value;
			} else {
				if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += Slicecolumn + " ";
					whereCondQuery += "IN";
					whereCondQuery += value;
				}
			}

			if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
					&& !"null".equalsIgnoreCase(whereCondQuery)) {
				whereCondQuery = " WHERE " + whereCondQuery;
			}
			JSONArray linksArr = new JSONArray();
			if (axisColumnsArr != null && !axisColumnsArr.isEmpty() && valueColumnsArr != null
					&& !valueColumnsArr.isEmpty()) {
				String groupBy = "";
				String tableName = "";
				String yAxisQuery = "";
				String fromColumn = (String) axisColumnsArr.get(0);
				String toColumn = (String) axisColumnsArr.get(1);
				String dataColumn = (String) valueColumnsArr.get(0);
				String dataColumn1 = (String) valueColumnsArr.get(0);
				if (!(aggregateColsObj != null && !aggregateColsObj.isEmpty()
						&& aggregateColsObj.get(dataColumn) != null
						&& !"".equalsIgnoreCase(String.valueOf(aggregateColsObj.get(dataColumn))))) {
					dataColumn = "SUM(" + dataColumn + ")";
				} else if (aggregateColsObj != null && !aggregateColsObj.isEmpty()
						&& !(aggregateColsObj.get(dataColumn) != null
								&& !"".equalsIgnoreCase(String.valueOf(aggregateColsObj.get(dataColumn))))) {
					dataColumn = "SUM(" + dataColumn + ")";
				}

				tableName = fromColumn.split("\\.")[0];
				yAxisQuery = "SELECT DISTINCT " + fromColumn + " FROM " + tableName + " " + whereCondQuery
						+ " ORDER BY " + fromColumn + " ASC";

				List yAxisSelectData = access.sqlqueryWithParams(yAxisQuery, new HashMap());
				if (yAxisSelectData != null && !yAxisSelectData.isEmpty()) {
					for (int i = 0; i < yAxisSelectData.size(); i++) {
						yAxisArr.add(yAxisSelectData.get(i));
					}
				}

				String fromTableName = fromColumn.split("\\.")[0];
				String toTableName = toColumn.split("\\.")[0];
				String dataQuery = "";
				if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
						&& !"null".equalsIgnoreCase(whereCondQuery)) {

					whereCondQuery += " AND " + fromColumn + " IS NOT NULL AND " + toColumn + " IS NOT NULL AND "
							+ dataColumn1 + " IS NOT NULL ";
				} else {
					whereCondQuery = " WHERE " + fromColumn + " IS NOT NULL AND " + toColumn + " IS NOT NULL AND "
							+ dataColumn1 + " IS NOT NULL ";
				}
				if (fromTableName != null && !"".equalsIgnoreCase(fromTableName) && toTableName != null
						&& !"".equalsIgnoreCase(toTableName) && fromTableName.equalsIgnoreCase(toTableName)) {
					tableName = fromTableName;
					groupBy = " GROUP BY " + fromColumn + " , " + toColumn;
					String columnNames = fromColumn + "," + toColumn + "," + dataColumn;
					dataQuery = "SELECT " + columnNames + " FROM " + tableName + " " + whereCondQuery + groupBy
							+ " ORDER BY " + fromColumn + " ASC";
				} else {
					groupBy = " GROUP BY " + fromColumn + " , " + toColumn;
					String columnNames = fromColumn + "," + toColumn + "," + dataColumn;
					dataQuery = "SELECT " + columnNames + " FROM " + fromTableName + ", " + toTableName + " "
							+ whereCondQuery + groupBy + " ORDER BY " + fromColumn + " ASC";

				}

				List selectData = access.sqlqueryWithParams(dataQuery, new HashMap());
				if (selectData != null && !selectData.isEmpty()) {
					JSONObject dataObject = new JSONObject();
					for (int i = 0; i < selectData.size(); i++) {
						Object[] objData = (Object[]) selectData.get(i);
						if (objData != null) {
							String regionData = (String) objData[0];
							String personData = (String) objData[1];
							if (dataObject != null && !dataObject.isEmpty() && dataObject.containsKey(personData)) {
								JSONObject dataObj = (JSONObject) dataObject.get(personData);
								dataObj.put(regionData, objData[2]);
								dataObject.put(personData, dataObj);
							} else {
								JSONObject dataObj = new JSONObject();
								dataObj.put(regionData, objData[2]);
								dataObject.put(personData, dataObj);
							}

						}
					}

					for (Object key : dataObject.keySet()) {
						String keyName = (String) key;
						JSONObject dataObj = (JSONObject) dataObject.get(keyName);
						JSONArray regionDataArr = new JSONArray();
						JSONArray dataObjKeys = new JSONArray();
						for (Object regionKey : dataObj.keySet()) {
							dataObjKeys.add(regionKey);
						}
						for (int j = 0; j < yAxisArr.size(); j++) {
							String dataKey = (String) yAxisArr.get(j);
							if (dataObjKeys != null && !dataObjKeys.isEmpty() && dataObjKeys.contains(dataKey)) {
								regionDataArr.add(dataObj.get(dataKey));
							} else {
								regionDataArr.add(0);
							}
						}
						finalDataObj.put(keyName, regionDataArr);
					}

				}

				chartObj.put("tableName", tableName);
			}
			chartObj.put("data", finalDataObj);
			chartObj.put("yAxisData", yAxisArr);
			chartObj.put("dataPropObject", dataPropObj);
			chartObj.put("layout", layoutObj);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	@Transactional
	public JSONObject getCardImageData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONArray dataFieldsArray = new JSONArray();
		JSONArray columnsArray = new JSONArray();
		JSONArray filtersColsArray = new JSONArray();
		JSONArray tablesArr = new JSONArray();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		Connection connection = null;

		int recordsCount = 0;
		try {
			JSONObject paramsDataObj = new JSONObject();
			String chartId = request.getParameter("chartId");
			String columnDataName = request.getParameter("columnName");
			String paramsData = request.getParameter("paramArray");
			String tables = request.getParameter("tablesObj");
			String filterConditionStr = request.getParameter("filterConditionArray");
			String tableName = "";
			String paramQuery = "SELECT VISUALIZE_CUST_COL14 FROM O_RECORD_VISUALIZATION WHERE CHART_ID =:CHART_ID AND ROLE_ID=:ROLE_ID AND ORGN_ID =:ORGN_ID";
			Map paramMap = new HashMap();
			paramMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
			paramMap.put("CHART_ID", chartId);
			paramMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
			List paramList = access.sqlqueryWithParams(paramQuery, paramMap);
			if (paramList != null && !paramList.isEmpty()) {
				paramsData = (String) paramList.get(0);
				if (paramsData != null && !"".equalsIgnoreCase(paramsData)) {
					paramsDataObj = (JSONObject) JSONValue.parse(paramsData);
				}
				if (paramsDataObj.containsKey("paramDateArr")) {
					LocalDate now = LocalDate.now();
					LocalDate earlier = now.minusMonths(1);
					LocalDate earlierDay = earlier.minusDays(1);
					LocalDate earlierMonth = earlierDay.minusMonths(1);

					DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
					String nowDate = now.format(dateTimeFormatter);
					String earlierDate = earlier.format(dateTimeFormatter);
					String earlierDayDate = earlierDay.format(dateTimeFormatter);
					String earlierMonthDate = earlierMonth.format(dateTimeFormatter);

					JSONObject paramFromObj = new JSONObject();
					paramFromObj.put("colName", columnDataName);
					paramFromObj.put("operator", "BETWEEN");
					paramFromObj.put("minvalue", earlierMonthDate);
					paramFromObj.put("maxvalue", earlierDayDate);
					JSONObject paramToObj = new JSONObject();
					paramToObj.put("colName", columnDataName);
					paramToObj.put("operator", "BETWEEN");
					paramToObj.put("minvalue", earlierDate);
					paramToObj.put("maxvalue", nowDate);
					JSONArray paramFromArr = new JSONArray();
					JSONArray paramToArr = new JSONArray();
					paramFromArr.add(paramFromObj);
					paramToArr.add(paramToObj);
					paramsDataObj = new JSONObject();
					paramsDataObj.put("paramFromArr", paramFromArr.toString());
					paramsDataObj.put("paramToArr", paramToArr.toString());

				}
			}

			if (chartId != null && !"".equalsIgnoreCase(chartId) && !"null".equalsIgnoreCase(chartId)) {
				String tableQuery = "SELECT TABLE_NAME FROM O_RECORD_VISUALIZATION WHERE CHART_ID=:CHART_ID "
						+ "AND ORGN_ID=:ORGN_ID AND ROLE_ID=:ROLE_ID";
				Map tableMap = new HashMap();
				tableMap.put("CHART_ID", chartId);
				tableMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
				tableMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
				List tableList = access.sqlqueryWithParams(tableQuery, tableMap);
				if (tableList != null && !tableList.isEmpty()) {
					tableName = (String) tableList.get(0);
					JSONArray paramArray = new JSONArray();
					if (tableName != null && !"".equalsIgnoreCase(tableName)) {
						for (Object objData : paramsDataObj.keySet()) {
							String paramFilterDataStr = (String) paramsDataObj.get(objData);
							JSONArray paramFilterDataArr = new JSONArray();
							if (paramFilterDataStr != null && !"".equalsIgnoreCase(paramFilterDataStr)) {
								paramFilterDataArr = (JSONArray) JSONValue.parse(paramFilterDataStr);
							}
							if (paramFilterDataArr != null && !paramFilterDataArr.isEmpty()) {
								paramArray.add(paramFilterDataArr.get(0));
							}
						}
						String gridQuery = "SELECT DISTINCT DAL_GRID.GRID_ID FROM DAL_GRID INNER JOIN DAL_GRID_ROLE_COL_LINK ON DAL_GRID.GRID_ID = DAL_GRID_ROLE_COL_LINK.GRID_ID WHERE DAL_GRID.GRID_REF_TABLE=:GRID_REF_TABLE AND DAL_GRID.ORGN_ID=:ORGN_ID "
								+ "AND DAL_GRID_ROLE_COL_LINK.ROLE_ID=:ROLE_ID";
						Map gridMap = new HashMap();
						gridMap.put("GRID_REF_TABLE", tableName);
						gridMap.put("ORGN_ID", request.getSession(false).getAttribute("ssOrgId"));
						gridMap.put("ROLE_ID", request.getSession(false).getAttribute("ssRole"));
						List gridList = access.sqlqueryWithParams(gridQuery, gridMap);
						if (gridList != null && !gridList.isEmpty()) {
							String gridId = (String) gridList.get(0);
							if (gridId != null && !"".equalsIgnoreCase(gridId)) {
								resultObj.put("gridId", gridId);
								resultObj.put("gridObj", getGrid(gridId, request));
								resultObj.put("paramArray", paramArray);
								return resultObj;
							}
						}
					}
				}
			}

			String groupscount = request.getParameter("groupscount");
			String pagenum = request.getParameter("pagenum");
			String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize") : "10";
			String recordendindex = request.getParameter("recordendindex");
			String recordstartindex = (request.getParameter("recordstartindex"));

			String getOnlyDataArray = (request.getParameter("getOnlyDataArray"));
			connection = DriverManager.getConnection(dbURL, userName, password);
			int startIndex = 0;
			int endIndex = 0;
			if (recordstartindex != null && recordendindex != null && pagesize != null) {
				startIndex = Integer.parseInt(recordstartindex);
				endIndex = Integer.parseInt(recordendindex);
			}

			String conditionQuery = "";
			Integer filterscount = 0;
			String filterCondition = "";
			String selectQuery = "";
			String whereCondQuery = "";
			String countQuery = "";
			String condition = "";
			if (paramsDataObj != null && !paramsDataObj.isEmpty()) {
				int j = 0;
				for (Object objData : paramsDataObj.keySet()) {
					String paramFilterDataStr = (String) paramsDataObj.get(objData);
					JSONArray paramFilterDataArr = new JSONArray();
					if (paramFilterDataStr != null && !"".equalsIgnoreCase(paramFilterDataStr)) {
						paramFilterDataArr = (JSONArray) JSONValue.parse(paramFilterDataStr);
					}
					if (paramFilterDataArr != null && !paramFilterDataArr.isEmpty()) {
						String paramQueryStr = getCardParamArr(request, (JSONObject) paramFilterDataArr.get(0),
								tableName);
						if (paramQueryStr != null && !"".equalsIgnoreCase(paramQueryStr)
								&& !"".equalsIgnoreCase(paramQueryStr)) {
							selectQuery += paramQueryStr;
							if (j < paramsDataObj.size() - 1) {
								selectQuery += " UNION ";
							}
							j++;
						}
					}
				}

				if (paramsData != null && !"".equalsIgnoreCase(paramsData)) {
					paramsDataObj = (JSONObject) JSONValue.parse(paramsData);
				}
			}

			if (paramsDataObj == null || paramsDataObj.isEmpty()) {
				if (!dashboardutils.isNullOrEmpty(tables)) {
					tablesArr = (JSONArray) JSONValue.parse(tables);
				}
				if (!dashboardutils.isNullOrEmpty(filterConditionStr)) {
					filtersColsArray = (JSONArray) JSONValue.parse(filterConditionStr);
				}

				if (filtersColsArray != null && !filtersColsArray.isEmpty()) {
					for (int i = 0; i < filtersColsArray.size(); i++) {
						JSONObject filterColObj = (JSONObject) filtersColsArray.get(i);
						if (filterColObj.get("values") instanceof JSONArray) {
							JSONArray valuesArr = (JSONArray) filterColObj.get("values");
							if (valuesArr != null && !valuesArr.isEmpty()) {
								String values = (String) valuesArr.stream().map(e -> e)
										.collect(Collectors.joining(","));
								filterColObj.put("values", values);
							}
						}

						if (filterColObj != null && !filterColObj.isEmpty()) {
							if (i == 0 && whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)) {
								whereCondQuery += " AND ";
							}
							whereCondQuery += buildCondition(filterColObj, request);
							if (i != filtersColsArray.size() - 1) {
								whereCondQuery += " AND ";
							}
						}
					}
				}

//				if (!dashboardutils.isNullOrEmpty(selectQuery) && !dashboardutils.isNullOrEmpty(whereCondQuery)) {
//					selectQuery = selectQuery + " AND " + whereCondQuery;
//				}

				if (dashboardutils.isNullOrEmpty(paramsData) && tablesArr != null && !tablesArr.isEmpty()) {
					tableName = (String) tablesArr.get(0);
					selectQuery = "SELECT * FROM " + tableName;

					if (!dashboardutils.isNullOrEmpty(whereCondQuery)) {
						selectQuery = selectQuery + " WHERE " + whereCondQuery;
					}

					if (request.getParameter("filterscount") != null) {
						filterscount = new Integer(request.getParameter("filterscount"));
						if (filterscount > 0) {
							filterCondition = buildFilterCondition(filterscount, request, dataBaseDriver);
							if (filterCondition != null && !"".equalsIgnoreCase(filterCondition)
									&& !"null".equalsIgnoreCase(filterCondition)) {
								if (selectQuery.contains("WHERE")) {
									selectQuery += " AND " + filterCondition;
								} else {
									selectQuery += " WHERE " + filterCondition;
								}
							}
						}
					}
				}
			}

			if (!dashboardutils.isNullOrEmpty(selectQuery)) {
				countQuery = "SELECT count(*) FROM (" + selectQuery + " )";
			}
			ResultSet countResultSet = null;
			if (!dashboardutils.isNullOrEmpty(countQuery)) {
				countResultSet = connection.prepareStatement(countQuery).executeQuery();
				while (countResultSet.next()) {
					recordsCount = countResultSet.getInt(1);
				}
			}
			if (!dashboardutils.isNullOrEmpty(selectQuery)) {
				selectQuery = "SELECT * FROM (" + selectQuery + ")";
			}
			String orderby = "";
			String sortdatafield = request.getParameter("sortdatafield");
			System.out.println("sortdatafield::::" + sortdatafield);
			String sortorder = request.getParameter("sortorder");
			if (!(sortdatafield != null && !"".equalsIgnoreCase(sortdatafield))) {
				sortdatafield = (String) request.getAttribute("sortdatafield");
			}
			if (!(sortorder != null && !"".equalsIgnoreCase(sortorder))) {
				sortorder = (String) request.getAttribute("sortorder");
			}
			System.out.println("sortorder::::" + sortorder);
			if (sortdatafield != null && sortorder != null && (sortorder.equals("asc") || sortorder.equals("desc"))) {
				orderby = " ORDER BY " + sortdatafield + " " + sortorder;
			}
			if (!dashboardutils.isNullOrEmpty(selectQuery)) {
				selectQuery += orderby;
			}
			if (dataBaseDriver.toUpperCase().contains("ORACLE")) {
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			} else if (dataBaseDriver.toUpperCase().contains("MYSQL")) {
				conditionQuery += " LIMIT " + startIndex + "," + pagesize + "";
			} else if (dataBaseDriver.toUpperCase().contains("MSSQL")) {
				if (!(orderby != null && !"".equalsIgnoreCase(orderby) && !"null".equalsIgnoreCase(orderby))) {
					selectQuery += " ORDER BY (SELECT NULL) ";
				}
				conditionQuery += " OFFSET " + startIndex + " ROWS FETCH NEXT " + pagesize + " ROWS ONLY";
			}
			if (!dashboardutils.isNullOrEmpty(selectQuery)) {
				selectQuery = selectQuery + conditionQuery;
			}
			int columnCount = 0;
			ResultSetMetaData resultSetMetaData = null;
			System.out.println("Tree Data query::" + selectQuery);
			if (!dashboardutils.isNullOrEmpty(selectQuery)) {
				preparedStatement = connection.prepareStatement(selectQuery);
				resultSet = preparedStatement.executeQuery();
				resultSetMetaData = resultSet.getMetaData();
				columnCount = resultSetMetaData.getColumnCount();
			}
			if (getOnlyDataArray != null && "Y".equalsIgnoreCase(getOnlyDataArray)) {
				if (resultSet != null) {
					while (resultSet.next()) {
						JSONObject dataObj = new JSONObject();

						for (int i = 1; i <= columnCount; i++) {
							JSONObject dataFieldsObj = new JSONObject();
							String columnType = resultSetMetaData.getColumnTypeName(i);
							String columnName = resultSetMetaData.getColumnName(i);
							Object data = null;
							if ("DATE".equalsIgnoreCase(columnType) || "DATETIME".equalsIgnoreCase(columnType)
									|| "TIMESTAMP".equalsIgnoreCase(columnType)) {
								data = resultSet.getString(columnName);
							} else if (columnType != null && !"".equalsIgnoreCase(columnType)
									&& "CLOB".equalsIgnoreCase(columnType)) {
								String popUpInsertString = new PilogUtilities()
										.clobToString((Clob) resultSet.getClob(columnName));
								if (popUpInsertString != null && !"".equalsIgnoreCase(popUpInsertString)) {
									data = popUpInsertString;
								}
							} else {
								data = resultSet.getObject(columnName);
							}
							if (data instanceof byte[]) {
								byte[] bytesArray = (byte[]) data;
								data = new RAW(bytesArray).stringValue();
							}
							dataObj.put(columnName, data);

						}

						dataArray.add(dataObj);

					}
				}
				if (recordsCount != 0) {
					dataArray.add(recordsCount);
				}

				resultObj.put("dataArray", dataArray);
			} else {
				for (int i = 1; i <= columnCount; i++) {
					JSONObject dataFieldsObj = new JSONObject();
					String columnType = resultSetMetaData.getColumnTypeName(i);
					String columnName = resultSetMetaData.getColumnName(i);
					dataFieldsObj.put("name", columnName);
					dataFieldsObj.put("type", "string");

					dataFieldsArray.add(dataFieldsObj);

					JSONObject columnsObject = new JSONObject();

					columnsObject.put("text", columnName);
					columnsObject.put("datafield", columnName);
					columnsObject.put("width", 120);
					columnsObject.put("sortable", true);
					columnsArray.add(columnsObject);

				}

				resultObj.put("dataFieldsArray", dataFieldsArray);
				resultObj.put("columnsArray", columnsArray);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;

	}

	public String getCardParamArr(HttpServletRequest request, JSONObject paramFilterData, String tableName) {
		Integer filterscount = 0;
		String filterCondition = "";
		JSONArray filtersColsArray = new JSONArray();
		String filterConditionStr = request.getParameter("filterConditionArray");
		String selectQuery = "SELECT * FROM " + tableName;
		try {
			String condition = "";
			if (paramFilterData != null && !paramFilterData.isEmpty()) {
				condition = buildCondition(paramFilterData, request);
			}
			if (condition != null && !"".equalsIgnoreCase(condition) && !"".equalsIgnoreCase(condition)) {
				if (selectQuery.contains("WHERE")) {
					selectQuery += " AND " + condition;
				} else {
					selectQuery += " WHERE " + condition;
				}
			}

			if (!dashboardutils.isNullOrEmpty(filterConditionStr)) {
				filtersColsArray = (JSONArray) JSONValue.parse(filterConditionStr);
			}

			if (filtersColsArray != null && !filtersColsArray.isEmpty()) {
				for (int i = 0; i < filtersColsArray.size(); i++) {
					JSONObject filterColObj = (JSONObject) filtersColsArray.get(i);
					if (filterColObj.get("values") instanceof JSONArray) {
						JSONArray valuesArr = (JSONArray) filterColObj.get("values");
						if (valuesArr != null && !valuesArr.isEmpty()) {
							String values = (String) valuesArr.stream().map(e -> e).collect(Collectors.joining(","));
							filterColObj.put("values", values);
						}
					}

					if (filterColObj != null && !filterColObj.isEmpty()) {
						if (i == 0 && condition != null && !"".equalsIgnoreCase(condition)) {
							condition = "";
						}
						condition += buildCondition(filterColObj, request);
						if (i != filtersColsArray.size() - 1) {
							condition += " AND ";
						}
					}
				}
				if (selectQuery.contains("WHERE")) {
					selectQuery += " AND " + condition;
				} else {
					selectQuery += " WHERE " + condition;
				}
			}

			if (request.getParameter("filterscount") != null) {
				filterscount = new Integer(request.getParameter("filterscount"));
				filterCondition = buildFilterCondition(filterscount, request, dataBaseDriver);
				if (filterCondition != null && !"".equalsIgnoreCase(filterCondition)
						&& !"null".equalsIgnoreCase(filterCondition)) {
					if (selectQuery.contains("WHERE")) {
						selectQuery += " AND " + filterCondition;
					} else {
						selectQuery += " WHERE " + filterCondition;
					}
				}

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return selectQuery;
	}

	@Transactional
	public int updateHomepageCardImg(HttpServletRequest request, JSONObject homepageCardImgChngEvtObj) {
		int updatecount = 0;

		try {
			String chartId = String.valueOf(homepageCardImgChngEvtObj.get("chartId"));
			String encodedCardImg = String.valueOf(homepageCardImgChngEvtObj.get("encodedCardImg"));
			String UpdateQuery = "UPDATE O_RECORD_VISUALIZATION SET VISUALIZE_CUST_COL17 =:ENCODED_STR WHERE CHART_ID =:CHART_ID";
			Map<String, Object> updateMap = new HashMap<>();
			updateMap.put("CHART_ID", chartId); // chartId
			updateMap.put("ENCODED_STR", encodedCardImg); // VISUALIZE_CUST_COL17
			updatecount = access.executeUpdateSQLNoAudit(UpdateQuery, updateMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return updatecount;
	}

	public String getBlankCellHeaderType(Sheet sheet, int rowNum, int cellNum, String headerType) {
		Row row = sheet.getRow(rowNum);
		if (row != null) {
			Cell cell = row.getCell(cellNum);
			if (cell != null) {
				switch (cell.getCellType()) {
					case Cell.CELL_TYPE_STRING:
						String dataFormatString = cell.getCellStyle().getDataFormatString();
						headerType = "VARCHAR2";
						break;
					case Cell.CELL_TYPE_BOOLEAN:
						break;
					case Cell.CELL_TYPE_NUMERIC:

						if (HSSFDateUtil.isCellDateFormatted(cell)) {
							String cellDateString = "";
							Date cellDate = cell.getDateCellValue();
							if ((cellDate.getYear() + 1900) == 1899 && (cellDate.getMonth() + 1) == 12
									&& (cellDate.getDate()) == 31) {
								cellDateString = (cellDate.getHours()) + ":" + (cellDate.getMinutes()) + ":"
										+ (cellDate.getSeconds());

							} else {
								cellDateString = (cellDate.getYear() + 1900) + "-" + (cellDate.getMonth() + 1) + "-"
										+ (cellDate.getDate());
							}

							headerType = "DATE";

						} else {
							String cellvalStr = NumberToTextConverter.toText(cell.getNumericCellValue());
							headerType = "NUMBER";
						}
						break;
					case Cell.CELL_TYPE_BLANK:
						rowNum++;
						headerType = getBlankCellHeaderType(sheet, rowNum, cellNum, headerType);

						break;
				}
			} else {
				rowNum++;
				headerType = getBlankCellHeaderType(sheet, rowNum, cellNum, headerType);
			}
		}

		return headerType;
	}

	@Transactional
	public JSONObject getUserTableNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String tableDiv = "";
			String userName = (String) request.getParameter("userName");
			if (userName != null && !"".equalsIgnoreCase(userName)) {
				String fetchQuery = "SELECT TABLE_NAME  FROM C_ETL_DAL_AUTHORIZATION WHERE CREATE_BY =:CREATE_BY";
				Map mapData = new HashMap();
				mapData.put("CREATE_BY", userName);
				List listData = access.sqlqueryWithParams(fetchQuery, mapData);
				if (listData != null && !listData.isEmpty()) {
					tableDiv = "<div id='userTableNamesDivId' class='userTableNamesDivClass text-right replyIntelisenseView noBubble'>"
							+ "<p class='nonLoadedBubble'>Existing Files/Tables</p>"
							+ "<div class=\"search nonLoadedBubble\">"
							+ "<input type=\"text\" placeholder=\"search\" id='data-search'/>" + "</div>"
							+ "<div id='userIntellisenseViewTableNamesDivId' class='userIntellisenseViewTableNamesDivClass nonLoadedBubble'>";
					for (int i = 0; i < listData.size(); i++) {
						String tableName = (String) listData.get(i);
						tableDiv += "<div id='" + tableName
								+ "_table' class='userTableNameClass' onclick=getConversationalAISelectedTableName('"
								+ tableName + "') data-intelliSenseViewTablefilter-item data-filter-name=\"" + tableName
								+ "\">" + tableName + "</div>";
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
	public JSONObject createIntelliSenseTableasFile(HttpServletRequest request, HttpServletResponse response,
			String mainFileName) {
		Connection connection = null;
		JSONObject resultObj = new JSONObject();
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			String batchNumber = "";
			String user = (String) request.getSession(false).getAttribute("ssUsername");
			String ssOrgName = (String) request.getSession(false).getAttribute("ssOrgName");
			String stgTable = request.getParameter("tableName");
			String newTableName = request.getParameter("newTableName");
			String fileName = request.getParameter("fileName");
			String filePath1 = request.getParameter("filePath");
			String deleteFlag = request.getParameter("deleteFlag");
			if (!(filePath1 != null && !"".equalsIgnoreCase(filePath1) && !"null".equalsIgnoreCase(filePath1))) {
				filePath1 = mainFileName;
			}
			if (stgTable != null && !"".equalsIgnoreCase(stgTable) && !"null".equalsIgnoreCase(stgTable)) {
				stgTable = stgTable.toUpperCase();
			}
			String filePath = fileStoreHomedirectory + "TreeDMImport/" + user + "/" + filePath1;
			List<String> headersList = dashboardutils.getHeadersOfImportedFile(request, response, filePath);
			List headerTypeList = getColumnTypesOfImportedFile(request, response, filePath);
			List toColumnsDataTypes = headerTypeList;
//            if (headerTypeList != null && !headerTypeList.isEmpty()) {
//                for (int i = 0; i < headerTypeList.size(); i++) {
//                    String headerName = (String) headerTypeList.get(i);
//                    toColumnsDataTypes.add(headerName);
//                }
//            }
			List dataList = dashboardutils.readExcelFile(request, filePath, fileName);
			JSONObject dbConnObj = new PilogUtilities().getDatabaseDetails(dataBaseDriver, dbURL, userName, password,
					"Current_V10");
			String insertQuery = "";
			connection = dashboardutils.getCurrentConnection();
			Map<Integer, Object> insertMap = new HashMap<>();
			List<Object[]> newDataList = new ArrayList();
			List dataTypesList = dashboardutils.getHeaderDataTypesOfImportedFile(request, filePath);
			List fromColumnsList = dashboardutils.fileHeaderValidations(headersList);
			if (deleteFlag != null && !"".equalsIgnoreCase(deleteFlag) && "Y".equalsIgnoreCase(deleteFlag)) {
				try {
					String deletequery = "DROP TABLE " + stgTable;
					preparedStatement = connection.prepareStatement(deletequery);
					preparedStatement.execute();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			String checkTableQuery = "SELECT TABLE_NAME FROM  USER_TABLES WHERE TABLE_NAME =:TABLE_NAME";
			Map checkMap = new HashMap();
			checkMap.put("TABLE_NAME", stgTable);
			List listData = access.sqlqueryWithParams(checkTableQuery, checkMap);
			if (listData != null && !listData.isEmpty()) {
				stgTable = (String) listData.get(0);
				String message = "Table is already existed with same Name .please change the Name";
				resultObj.put("message", message);
				resultObj.put("tableName", stgTable);
				resultObj.put("fileName", fileName);
				resultObj.put("filePath", filePath1);
				return resultObj;
			} else {
				fromColumnsList.add("AUDIT_ID");
				dataTypesList.add("VARCHAR2(100 CHAR)               DEFAULT '" + stgTable.toUpperCase()
						+ "'||SYS_GUID() NOT NULL");

				String createTableQuery = "CREATE TABLE " + stgTable + "( ";
				for (int i = 0; i < fromColumnsList.size(); i++) {
					if (i < (fromColumnsList.size() - 1)) {
						createTableQuery += fromColumnsList.get(i) + " " + dataTypesList.get(i) + " , ";
					} else {
						createTableQuery += fromColumnsList.get(i) + " " + dataTypesList.get(i) + ")";
					}
				}
				preparedStatement = connection.prepareStatement(createTableQuery);
				preparedStatement.execute();
				fromColumnsList.remove("AUDIT_ID");
				String columnsStr = (String) fromColumnsList.stream().map(e -> e).collect(Collectors.joining(","));
				String paramsStr = (String) fromColumnsList.stream().map(e -> "?").collect(Collectors.joining(","));
				insertQuery = "INSERT INTO " + stgTable + " (" + columnsStr + " ) VALUES ( " + paramsStr + ")";

				preparedStatement = connection.prepareStatement(insertQuery);
				int insertCount = insertDataIntoTable(request, stgTable, preparedStatement, headersList, dataList,
						headersList, toColumnsDataTypes, "ORACLE", null);
				String message = insertCount + " Records Imported with Batch no " + batchNumber;
				resultObj.put("message", message);
				resultObj.put("tableName", stgTable);

				String insertTbaleQuery = "INSERT INTO C_ETL_DAL_AUTHORIZATION(TABLE_NAME,CREATE_BY) VALUES('"
						+ stgTable + "','" + user + "')";
				try {
					int cnt = access.executeUpdateSQL(insertTbaleQuery); 
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;

	}

	@Transactional
	public JSONObject getIntelliSenseTableColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String columnDiv = "";
			String tableName = request.getParameter("tableName");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				tableName = tableName.toUpperCase();
				String columnQuery = "SELECT COLUMN_NAME  TABLE_NAME  FROM  USER_TAB_COLUMNS WHERE TABLE_NAME=:TABLE_NAME";
				Map columnMap = new HashMap();
				columnMap.put("TABLE_NAME", tableName);
				List listData = access.sqlqueryWithParams(columnQuery, columnMap);
				if (listData != null && !listData.isEmpty()) {
					columnDiv = "<div id='userColumnNamesDivId' class='userColumnNamesDivClass text-left'>";

					for (int i = 0; i < listData.size(); i++) {
						String columnName = (String) listData.get(i);
						columnDiv += "<div id='" + columnName
								+ "_table' class='convai-left-message userColumnNameClass'>" + columnName + "</div>";
					}
					columnDiv += "</div>";
					resultObj.put("columnDiv", columnDiv);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getIntelliSenseChartTypes(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String replyId = request.getParameter("replyId");
			System.out.println("replyId::" + replyId);
			String chartDiv = "<div id=\"visionVisualizeBasicTabs\" class=\"visionVisualizeChartsTabsClass nonLoadedBubble\">"
					+ "<div class=\"row iconsRow\">" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Pie.svg', 'pie'," + replyId
					+ " )\" src=\"images/Pie.svg\" class=\"visualDarkMode\" title=\"Pie chart looks like circle it is divided into sectors that each represent a proportion of the whole.\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Bar.svg', 'bar'," + replyId
					+ ")\" src=\"images/Bar.svg\" class=\"visualDarkMode\" title=\"A bar chart is a chart that presents categorical data with rectangular bars with lengths proportional to the values that they represent. The bars can be plotted horizontally\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Donut.svg', 'donut'," + replyId
					+ ")\" src=\"images/Donut.svg\" class=\"visualDarkMode\" title=\"Doughnut chart looks like circle with hole it is divided into sectors that each represent a proportion of the whole\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Column.svg', 'column'," + replyId
					+ ")\" src=\"images/Column.svg\" class=\"visualDarkMode\" title=\"A column chart is a chart that presents categorical data with rectangular bars with heights proportional to the values that they represent. The bars can be plotted vertically\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Line.svg', 'lines'," + replyId
					+ ")\" src=\"images/Line.svg\" class=\"visualDarkMode\" title=\"A line chart is a type of chart which displays information as a series of data points called \" markers'=\"\" connected=\"\" by=\"\" straight=\"\" line=\"\" segments'=\"\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Scatter.svg', 'scatter'," + replyId
					+ ")\" src=\"images/Scatter.svg\" class=\"visualDarkMode\" title=\"Scatter chart\">" + "</div>"
					+ "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Tree_Chart.svg', 'treemap'," + replyId
					+ ")\" src=\"images/Tree_Chart.svg\" class=\"visualDarkMode\" title=\"Tree maps display hierarchical data as a set of nested rectangles. Each branch of the tree is given a rectangle, which is then tiled with smaller rectangles representing sub-branches\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Histogram.svg', 'column'," + replyId
					+ ")\" src=\"images/Histogram.svg\" class=\"visualDarkMode\" title=\"Histogram chart\">" + "</div>"
					+ "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Guage.svg', 'indicator'," + replyId
					+ ")\" src=\"images/Guage.svg\" class=\"visualDarkMode\" title=\"Guage chart\">" + "</div>"
					+ "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Funnel.svg', 'funnel'," + replyId
					+ ")\" src=\"images/Funnel.svg\" class=\"visualDarkMode\" title=\"Funnel charts can be used to illustrate stages in a process, they could be used to show anything that’s decreasing in size\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Candlestick.svg', 'candlestick'," + replyId
					+ ")\" src=\"images/Candlestick.svg\" class=\"visualDarkMode\" title=\"Candlestick chart\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Waterfall.svg', 'waterfall'," + replyId
					+ ")\" src=\"images/Waterfall.svg\" class=\"visualDarkMode\" title=\"A waterfall chart is a form of data visualization that helps in understanding the cumulative effect of sequentially introduced positive or negative values. These intermediate values can either be time based or category based\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Redar-Chart.svg', 'scatterpolar'," + replyId
					+ ")\" src=\"images/Redar-Chart.svg\" class=\"visualDarkMode\" title=\"A radar chart is a graphical method of displaying multivariate data in the form of a two-dimensional chart of three or more quantitative variables represented on axes starting from the same point\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('HeatMap.svg', 'heatMap'," + replyId
					+ ")\" src=\"images/HeatMap.svg\" class=\"visualDarkMode\" title=\"A heat map is a data visualization technique that shows magnitude of a phenomenon as color in two dimensions. The variation in color may be by hue or intensity, giving obvious visual cues to the reader about how the phenomenon is clustered or varies over space\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Bar.svg', 'barRotation'," + replyId
					+ ")\" src=\"images/Bar.svg\" class=\"visualDarkMode\" title=\"Bar Label Rotation chart\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Sunburst.svg', 'sunburst'," + replyId
					+ ")\" src=\"images/Sunburst.svg\" class=\"visualDarkMode\" title=\"The sunburst chart is ideal for displaying hierarchical data. Each level of the hierarchy is represented by one ring or circle with the innermost circle as the top of the hierarchy\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('GeoChart.svg', 'geochart'," + replyId
					+ ")\" src=\"images/GeoChart.svg\" class=\"visualDarkMode\" title=\"Geo chart\">" + "</div>"
					+ "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Bar_Chart_Inner_Icon.svg', 'BarAndLine'," + replyId
					+ ")\" src=\"images/Bar_Chart_Inner_Icon.svg\" class=\"visualDarkMode\" title=\"Bar and Line chart\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Integral-Analytics-Icon.png', 'boxplot'," + replyId
					+ ")\" src=\"images/Integral-Analytics-Icon.png\" class=\"visualDarkMode\" title=\"Box Plot\">"
					+ "</div>" + "<div class=\"col-lg-4  col-md-4 visualIconDivImg\">"
					+ "<img onclick=\"showIntelliSenseViewChartDiv('Sunburst.svg', 'sankey'," + replyId
					+ ")\" src=\"images/Sunburst.svg\" class=\"visualDarkMode\" title=\"The sunburst chart is ideal for displaying hierarchical data. Each level of the hierarchy is represented by one ring or circle with the innermost circle as the top of the hierarchy\">"
					+ "</div>" + "</div>" + "</div>";
			resultObj.put("chartDiv", chartDiv);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getIntelliSenseChartColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String chartColumnDiv = "";
			String tableName = request.getParameter("tableName");
			String chartType = request.getParameter("chartType");
			String columnsCount = request.getParameter("columnsCount");
			List axisChartTypes = new ArrayList();
			axisChartTypes.add("treemap");
			axisChartTypes.add("sunburst");
			List valuesChartTypes = new ArrayList();
			valuesChartTypes.add("bar");
			valuesChartTypes.add("column");
			valuesChartTypes.add("lines");
			valuesChartTypes.add("scatter");
			valuesChartTypes.add("funnel");
			valuesChartTypes.add("lines");
			valuesChartTypes.add("lines");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				tableName = tableName.toUpperCase();
				String columnQuery = "SELECT COLUMN_NAME FROM  USER_TAB_COLUMNS WHERE TABLE_NAME=:TABLE_NAME";
				Map columnMap = new HashMap();
				columnMap.put("TABLE_NAME", tableName);
				String inputColumnName = "";
				List listData = access.sqlqueryWithParams(columnQuery, columnMap);
				if (listData != null && !listData.isEmpty()) {
					chartColumnDiv = "<div id='intelliSenseViewColumnsId" + columnsCount
							+ "' class='intelliSenseViewColumnsClass convai-left-message'>";
					if (chartType != null && !"".equalsIgnoreCase(chartType)
							&& !"indicator".equalsIgnoreCase(chartType)) {
						chartColumnDiv += "<div id='intelliSenseViewAxisColumnsId" + columnsCount
								+ "' class='intelliSenseViewAxisColumnsClass nonLoadedBubble' style='display:none'>"
								+ "<span class='intelliSenseViewColumnsSpanCLass'>Axis :</span>"
								+ "<div id='intelliSenseViewAxisSelectInputColumnsId" + columnsCount
								+ "0' class='intelliSenseViewAxisSelectInputColumnsClass'>"
								+ "<select id='userAxisColumnNamesDivId" + columnsCount
								+ "0' onchange=getIntelliSenseAxisColumn(this," + columnsCount + ",0)>";
						for (int i = 0; i < listData.size(); i++) {
							String columnName = (String) listData.get(i);
							if (i == 0) {
								inputColumnName = tableName + "." + columnName;
							}
							chartColumnDiv += "<option id='" + columnName + "_table' value='" + columnName + "'>"
									+ columnName + "</option>";
						}
						chartColumnDiv += "</select>" + "<input type='text' id='intellisenseViewAxisInputId"
								+ columnsCount + "0' readonly value='" + inputColumnName + "'/>" + "</div>";
						if (chartType != null && !"".equalsIgnoreCase(chartType) && !"".equalsIgnoreCase(chartType)
								&& axisChartTypes.contains(chartType)) {
							chartColumnDiv += "<img src=\"images/Plus_White_Icon.svg\" title=\"Add Columns\" onclick=\"getIntelliSenseViewAddColumns(this,"
									+ "'intelliSenseViewAxisColumnsId" + columnsCount + "'," + "'" + tableName + "','"
									+ inputColumnName + "','" + columnsCount + "','AXIS','" + chartType
									+ "')\" class=\"intellisenseviewaddvaluecolumns\" style=\"display: inline; \">";
						}
						chartColumnDiv += "</div>";
					}
					chartColumnDiv += "<div id='intelliSenseViewValuesColumnsId" + columnsCount
							+ "' class='intelliSenseViewValuesColumnsClass convai-left-message nonLoadedBubble' style='display:none'>"
							+ "<span class='intelliSenseViewColumnsSpanCLass'>Values :</span>"
							+ "<div id='intelliSenseViewValuesSelectInputColumnsId" + columnsCount
							+ "0' class='intelliSenseViewValuesSelectInputColumnsClass '>"
							+ "<select id='userValuesColumnNamesDivId" + columnsCount
							+ "0' onchange=getIntelliSenseValuesColumn(this," + columnsCount + ",0)>";
					for (int i = 0; i < listData.size(); i++) {
						String columnName = (String) listData.get(i);
						chartColumnDiv += "<option id='" + columnName + "_table' value='" + columnName + "'>"
								+ columnName + "</option>";
					}
					String aggColuumnName = "intellisenseViewValuesInputId" + columnsCount + "0";
					chartColumnDiv += "</select>" + "<input type='text' id='intellisenseViewValuesInputId"
							+ columnsCount + "0' readonly value='" + inputColumnName + "'/>"
							+ "<img id='intellisenseViewValuesAggregateId" + columnsCount
							+ "0' src=\"images/Horizontal_Dots.svg\" title=\"Aggregate Functions\" onclick=\"getIntelliSenseViewAggregateFunctions(this,'"
							+ aggColuumnName + "','visionVisualizeChartId" + columnsCount + "','" + columnsCount
							+ "',0,'" + inputColumnName + "','" + tableName
							+ "')\" class=\"visionAggregateColumnBtn\" style=\"display: inline; \">" + "</div>";
					if (chartType != null && !"".equalsIgnoreCase(chartType) && !"".equalsIgnoreCase(chartType)
							&& valuesChartTypes.contains(chartType)) {
						chartColumnDiv += "<img src=\"images/Plus_White_Icon.svg\" title=\"Add Columns\" onclick=\"getIntelliSenseViewAddColumns(this,"
								+ "'intelliSenseViewValuesColumnsId" + columnsCount + "'," + "'" + tableName + "','"
								+ inputColumnName + "','" + columnsCount + "','VALUES','" + chartType
								+ "')\" class=\"intellisenseviewaddvaluecolumns\" style=\"display: inline; \">";
					}
					chartColumnDiv += "</div>";
					chartColumnDiv += "</div>";
					resultObj.put("chartColumnsDiv", chartColumnDiv);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getIntelliSenseExampleChartDesign(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String chartColumnDiv = "";
			String chartType = request.getParameter("chartType");
			if (chartType != null && !"".equalsIgnoreCase(chartType)) {
				String columnQuery = "SELECT CHART_DESCRIPTION,  CHART_IMAGE,DATA_IMAGE, AGGREGATE_IMAGE  FROM  IG_SMART_VIEW WHERE CHART_TYPE=:CHART_TYPE";
				Map columnMap = new HashMap();
				columnMap.put("CHART_TYPE", chartType);
				List listData = access.sqlqueryWithParams(columnQuery, columnMap);
				if (listData != null && !listData.isEmpty()) {
					Object[] objData = (Object[]) listData.get(0);
					String chartImgstring = "";
					String dataImgstring = "";
					String aggregateImgstring = "";
					if (objData != null) {
						String description = (String) objData[0];
						chartImgstring = getBlobString(objData[1]);
						dataImgstring = getBlobString(objData[2]);
						aggregateImgstring = getBlobString(objData[3]);
						chartColumnDiv = "<div id='intelliSenseViewExampleChartId' class='intelliSenseViewExampleChartClass'>"
								+ "<div id='intelliSenseViewExampleChartDescriptionId' class='intelliSenseViewExampleChartDescriptionClass' style='display:none'>"
								+ description + "</div>"
								+ "<div id='intelliSenseViewExampleChartImageId' class='intelliSenseViewExampleChartImageClass' style='display:none'>"
								+ "<img src='" + chartImgstring + "'/>" + "</div>"
								+ "<div id='intelliSenseViewExampleChartDataImageId' class='intelliSenseViewExampleChartDataImageClass' style='display:none'>"
								+ "<img src='" + dataImgstring + "'/>" + "</div>"
								+ "<div id='intelliSenseViewExampleChartAggregateImageId' class='intelliSenseViewExampleChartAggregateImageClass' style='display:none'>"
								+ "<img src='" + aggregateImgstring + "'/>" + "</div>" + "</div>";
						resultObj.put("chartColumnsDiv", chartColumnDiv);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	public String getBlobString(Object objData) {
		String imgStr = "";
		try {
			Blob chartblob = (Blob) objData;
			if (chartblob != null) {
				Long blob_len = chartblob.length();
				byte[] byteArray = new byte[blob_len.intValue()];
				chartblob.getBinaryStream().read(byteArray);
				String filestring = Base64.getEncoder().encodeToString(byteArray);
				imgStr = "data:image/gif;base64," + filestring;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return imgStr;
	}

	@Transactional
	public JSONObject getIntelliSenseChartSubColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String chartColumnDiv = "";
			String tableName = request.getParameter("tableName");
			String columnsCount = request.getParameter("columnsCount");
			String chartCount = request.getParameter("chartCount");
			String columnType = request.getParameter("columnType");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				String columnQuery = "SELECT COLUMN_NAME FROM  USER_TAB_COLUMNS WHERE TABLE_NAME=:TABLE_NAME";
				Map columnMap = new HashMap();
				columnMap.put("TABLE_NAME", tableName);
				String inputColumnName = "";
				List listData = access.sqlqueryWithParams(columnQuery, columnMap);
				if (listData != null && !listData.isEmpty()) {
					if (columnType != null && !"".equalsIgnoreCase(columnType) && "AXIS".equalsIgnoreCase(columnType)) {
						chartColumnDiv = "<div id='intelliSenseViewAxisSelectInputColumnsId" + chartCount + ""
								+ columnsCount + "' " + "class='intelliSenseViewAxisSelectInputColumnsClass '>"
								+ "<select id='userAxisColumnNamesDivId" + chartCount + "" + columnsCount + "' "
								+ " onchange=getIntelliSenseAxisColumn(this," + chartCount + "," + columnsCount + ")>";
						for (int i = 0; i < listData.size(); i++) {
							String columnName = (String) listData.get(i);
							chartColumnDiv += "<option id='" + columnName + "_table' value='" + columnName + "'>"
									+ columnName + "</option>";
						}
						chartColumnDiv += "</select>" + "<input type='text' id='intellisenseViewAxisInputId"
								+ chartCount + "" + columnsCount + "' readonly value='" + inputColumnName + "'/>"
								+ "<img id='intellisenseViewAxisDeleteId" + columnsCount + "" + columnsCount
								+ "' src=\"images/delete_icon.svg\"  title=\"Delete Columns\" onclick=\"getIntelliSenseViewConvAIDeleteColumns(this,"
								+ "'intelliSenseViewAxisSelectInputColumnsId" + chartCount + "" + columnsCount
								+ "')\" class=\"visionDeleteColumnBtn\" style=\"display: inline; \">" + "</div>";
					}
					if (columnType != null && !"".equalsIgnoreCase(columnType)
							&& "VALUES".equalsIgnoreCase(columnType)) {
						chartColumnDiv = "<div id='intelliSenseViewValuesSelectInputColumnsId" + chartCount + ""
								+ columnsCount + "' " + "class='intelliSenseViewValuesSelectInputColumnsClass '>"
								+ "<select id='userValuesColumnNamesDivId" + chartCount + "" + columnsCount + "' "
								+ "onchange=getIntelliSenseValuesColumn(this," + chartCount + "," + columnsCount + ")>";
						for (int i = 0; i < listData.size(); i++) {
							String columnName = (String) listData.get(i);
							chartColumnDiv += "<option id='" + columnName + "_table' value='" + columnName + "'>"
									+ columnName + "</option>";
						}
						String aggColuumnName = "intellisenseViewValuesInputId" + chartCount + "" + columnsCount;
						chartColumnDiv += "</select>" + "<input type='text' id='intellisenseViewValuesInputId"
								+ chartCount + "" + columnsCount + "' readonly value='" + inputColumnName + "'/>"
								+ "<img id='intellisenseViewValuesAggregateId" + columnsCount + "" + columnsCount
								+ "' src=\"images/Horizontal_Dots.svg\"  title=\"Aggregate Functions\" onclick=\"getIntelliSenseViewAggregateFunctions(this,'"
								+ aggColuumnName + "','visionVisualizeChartId" + chartCount + "'," + chartCount + ","
								+ columnsCount + ",'" + inputColumnName + "','" + tableName
								+ "','changeFlag')\" class=\"visionAggregateColumnBtn\" style=\"display: inline; \">"
								+ "<img id='intellisenseViewValuesDeleteId" + columnsCount + "" + columnsCount
								+ "' src=\"images/delete_icon.svg\"  title=\"Delete Columns\" onclick=\"getIntelliSenseViewConvAIDeleteColumns(this,"
								+ "'intelliSenseViewValuesSelectInputColumnsId" + chartCount + "" + columnsCount
								+ "')\" class=\"visionDeleteColumnBtn\" style=\"display: inline; \">" + "</div>";
					}
					resultObj.put("chartColumnsDiv", chartColumnDiv);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getIntelliSenseViewFilters(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String chartColumnDiv = "";
			String tableName = request.getParameter("tableName");
			String chartCount = request.getParameter("count");
			String columnsCount = request.getParameter("columnsCount");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				String columnQuery = "SELECT COLUMN_NAME FROM  USER_TAB_COLUMNS WHERE TABLE_NAME=:TABLE_NAME";
				Map columnMap = new HashMap();
				columnMap.put("TABLE_NAME", tableName);
				String inputColumnName = "";
				List listData = access.sqlqueryWithParams(columnQuery, columnMap);
				if (listData != null && !listData.isEmpty()) {
					chartColumnDiv = "<div id='intelliSenseViewFilterColumnsId" + chartCount + "" + columnsCount + "' "
							+ "class='intelliSenseViewFilterSelectInputColumnsClass noleftmargin'>"
							+ "<select id='userFilterColumnNamesDivId" + chartCount + "" + columnsCount + "' "
							+ " onchange=getIntelliSenseFilterColumn(this," + chartCount + "," + columnsCount + ")>";
					chartColumnDiv += "<option id='select_table' value='select'>Select</option>";
					for (int i = 0; i < listData.size(); i++) {
						String columnName = (String) listData.get(i);
						chartColumnDiv += "<option id='" + columnName + "_table' value='" + columnName + "'>"
								+ columnName + "</option>";
					}
					chartColumnDiv += "</select>" + "<div id='intellisenseViewFilterDivId" + chartCount + ""
							+ columnsCount + "' class='intellisenseViewFilterDivClass'/>" + "</div>";
					resultObj.put("chartColumnsDiv", chartColumnDiv);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getIntelliSenseViewFiltersValues(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String result = "";
			String tableName = request.getParameter("tableName");
			String columnName = request.getParameter("selectedVal");
			String chartCount = request.getParameter("count");
			String columnsCount = request.getParameter("columnsCount");
			JSONArray checkBoxDataArr = new JSONArray();
			JSONObject labelsObj = new PilogUtilities().getMultilingualObject(request);
			String operators = "<select id ='visionVisualizeChartFiltersFieldOperatorsId" + chartCount + ""
					+ columnsCount + "' class='visionVisualizeChartFiltersOperatorsClass'>" + "<option value= 'IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "In") + "</option>"
					+ "<option value= 'Containing'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Containing") + "</option>"
					+ "<option value= 'EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Equals") + "</option>"
					+ "<option value= 'LIKE'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Like")
					+ "</option>" + "<option value= 'BEGINING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Beginning With") + "</option>"
					+ "<option value= 'ENDING WITH'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Ending With") + "</option>"
					+ "<option value= 'NOT EQUALS'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Equals") + "</option>"
					+ "<option value= 'NOT IN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not In") + "</option>"
					+ "<option value= 'IS'>" + new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is")
					+ "</option>" + "<option value= 'IS NOT'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Is Not") + "</option>"
					+ "<option value= 'NOT LIKE'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Not Like") + "</option>"
					+ "<option value= 'BETWEEN'>"
					+ new PilogUtilities().convertIntoMultilingualValue(labelsObj, "Between") + "</option>"
					+ "</select>";
			if (tableName != null && !"".equalsIgnoreCase(tableName) && columnName != null
					&& !"".equalsIgnoreCase(columnName)) {
				String columnQuery = "SELECT DISTINCT " + columnName + " FROM " + tableName + " ";
				Map columnMap = new HashMap();
				String inputColumnName = "";
				List listData = access.sqlqueryWithParams(columnQuery, columnMap);
				if (listData != null && !listData.isEmpty()) {
					result = "<div id ='visionVisualizeChartFiltersFieldDivId" + chartCount + "" + columnsCount
							+ "' class='visionVisualizeChartFiltersFieldDivClass'>"
							+ "<div class='visionVisualizeChartFiltersFieldOperator'> <div id ='visionVisualizeChartFiltersFieldId"
							+ chartCount + "" + columnsCount + "' class='visionVisualizeChartFiltersFieldsClass'>"
							+ "<span class='visionVisualizeChartFiltersFieldSpan'>" + columnName
							+ "</span><img src='images/close_white.png' title=\"Remove Column\" onclick=\"RemoveFilterColumns('"
							+ chartCount + "','','" + columnsCount + "')\"/></div>"
							+ "<div id ='visionVisualizeChartFiltersFieldOperatorsDivId" + chartCount + ""
							+ columnsCount + "' class='visionVisualizeChartFiltersFieldOperatorsClass'>" + operators
							+ "</div></div>" + "<div id ='visionVisualizeChartFiltersFieldValuesId" + chartCount + ""
							+ columnsCount + "' class='visionVisualizeChartFiltersFieldValuesClass' >";
					for (int i = 0; i < listData.size(); i++) {
						String checkBoxValue = "";
						if (listData.get(i) instanceof String) {
							checkBoxValue = (String) listData.get(i);
							if (checkBoxValue != null && !"".equalsIgnoreCase(checkBoxValue)
									&& !"null".equalsIgnoreCase(checkBoxValue)) {
								checkBoxValue = checkBoxValue.trim();
							}
						}
						JSONObject checkBoxData = new JSONObject();
						checkBoxData.put("text", checkBoxValue);
						checkBoxData.put("value", checkBoxValue);
						checkBoxDataArr.add(checkBoxData);

					}
					result += "</div>" + "</div>";
				}
			}
			resultObj.put(columnName, result);
			resultObj.put("checkBoxList", checkBoxDataArr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getEditorMergeTableNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String tableDiv = "<div id='userEditorMergeTablesDivId' class='userEditorMergeTablesDivClass'>";
			String query = "SELECT TABLE_NAME FROM  USER_TABLES ORDER BY TABLE_NAME";
			List listData = access.sqlqueryWithParams(query, new HashMap());
			if (listData != null && !listData.isEmpty()) {
				tableDiv += "<div class='userEditorMergeDivClass'>";
				tableDiv += "<input type='text' id='mergeTableFilterId' class='visionmergerTablesClass'/>";
				tableDiv += "<ul id='userEditorMergeTablesId' class='userEditorMergeTablesClass'>";
				for (int i = 0; i < listData.size(); i++) {
					String tableName = (String) listData.get(i);
					tableDiv += "<li id='" + tableName + "_table' title='" + tableName
							+ "' class='userEditorMergeTableClass'>" + "<span class='columnAddTableName'>" + tableName
							+ "</span>"
							+ "<span class='columnAddImg'><img src='images/image2vector.svg' class='addcolumnIcon' onclick=\"mergeTablesAddFilter('"
							+ tableName + "')\"></span>" + "</li>";
				}
				tableDiv += "</ul>";
				tableDiv += "</div>";
				tableDiv += "<div id='userEditorMergeTablesAppendId' class='userEditorMergeTablesAppendClass'>"
						+ "<ul class='userEditorMergeTablesAppendUlClass'>" + "</ul>" + "</div>" + "</div>";
				tableDiv += "<div class='userEditorMergeTablesErrorDivClass'></div>";
			}
			resultObj.put("tableDiv", tableDiv);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getEditorMergeTableColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		try {
			List columnsNamesList = new ArrayList();
			JSONObject columnTypeObj = new JSONObject();
			JSONArray tablesArr = new JSONArray();
			String newTableName = request.getParameter("newTableName");
			String tablesStr = request.getParameter("tablesArr");
			if (tablesStr != null && !"".equalsIgnoreCase(tablesStr) && !"null".equalsIgnoreCase(tablesStr)) {
				tablesArr = (JSONArray) JSONValue.parse(tablesStr);
			}
			if (tablesArr != null && !tablesArr.isEmpty()) {

				for (int i = 0; i < tablesArr.size(); i++) {
					Class.forName(dataBaseDriver);
					connection = DriverManager.getConnection(dbURL, userName, password);
					Statement statement = connection.createStatement();
					ResultSet results = statement.executeQuery("SELECT * FROM " + tablesArr.get(i) + "");
					ResultSetMetaData metadata = results.getMetaData();
					int columnCount = metadata.getColumnCount();
					if (columnCount > 0) {
						for (int j = 1; j <= columnCount; j++) {
							String columnName = metadata.getColumnName(j);
							String columnType = metadata.getColumnTypeName(j);
							String tableColName = tablesArr.get(i) + "." + columnName;
							columnsNamesList.add(tableColName);
							columnTypeObj.put(tableColName, columnType);
						}
					}

				}

			}
			String tableDiv = "<div id='editorMergeTableSoucreDestiColumnsDivId' class='editorMergeTableSoucreDestiColumnsDivClass'>"
					+ "<div id='editorMergeTableColumnsDivId' class='editorMergeTableColumnsDivClass'>";
			if (columnsNamesList != null && !columnsNamesList.isEmpty()) {
				tableDiv += "<table id='editorMergeTableColumnsTableId' class='editorMergeTableColumnsTableClass table-bordered'>";
				tableDiv += "<thead>";
				tableDiv += "<tr><th class='addTableColumnheader'></th><th class='addTableColumnheader'>Source Columns</th><th class='addTableColumnheader'>Destination Columns</th><th>Data Types</th></tr>";
				tableDiv += "</thead><tbody>";
				for (int i = 0; i < columnsNamesList.size(); i++) {
					String tableColName = (String) columnsNamesList.get(i);
					String colName = tableColName.split("\\.")[1];
					String newTableColName = colName;
					String colType = (String) columnTypeObj.get(tableColName);
					if (colType != null && !"".equalsIgnoreCase(colType) && "VARCHAR2".equalsIgnoreCase(colType)) {
						colType = colType + "(4000)";
					} else if (colType != null && !"".equalsIgnoreCase(colType) && "CHAR".equalsIgnoreCase(colType)) {
						colType = colType + "(100)";
					}
					tableDiv += "<tr id='trid" + i + "'>"
							+ "<td><img src=\"images/Detele Red Icon.svg\" onclick=\"deleteMergeColumnSelectedRow(this,'trid"
							+ i
							+ "')\" class=\"mergeColMappingImg\" title=\"Delete\" style=\"width:15px;height: 15px;cursor:pointer;\"></td>"
							+ "<td><input type='text' class='mergeColSourceClass' value='" + tableColName
							+ "' readonly/></td>" + "<td><input type='text' class='mergeColDestinationClass' value='"
							+ newTableColName + "'/></td>" + "<td><input type='text' class='mergeColTypeClass' value='"
							+ colType + "'/></td>" + "</tr>";
				}
				tableDiv += "</tbody>";
				tableDiv += "</table>";

			}
			tableDiv += "</div>";
			tableDiv += "<div id='userEditorMergeTableDuplicateColsErrorId' class='userEditorMergeTableDuplicateColsErrorClass'></div>";
			tableDiv += "</div>";
			resultObj.put("tableDiv", tableDiv);

			if (connection != null) {
				connection.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return resultObj;
	}

	@Transactional
	public JSONObject checkExistMergeTableName(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		try {
			String tableName = request.getParameter("newTableName");
			if (tableName != null && !"".equalsIgnoreCase(tableName) && !"null".equalsIgnoreCase(tableName)) {
				Class.forName(dataBaseDriver);
				connection = DriverManager.getConnection(dbURL, userName, password);
				Statement statement = connection.createStatement();
				ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
				ResultSetMetaData metadata = results.getMetaData();
				int columnCount = metadata.getColumnCount();
				if (columnCount > 0) {
					resultObj.put("Message", "Table is already existed");
				} else {
					resultObj.put("Message", "Table is not existed");
				}
			}
			if (connection != null) {
				connection.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			resultObj.put("Message", "Table is not existed");
		}
		return resultObj;
	}

	@Transactional
	public JSONObject createTableANdJoinTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		try {
			JSONObject destiColObj = new JSONObject();
			String destiColTypes = request.getParameter("destiColTypes");
			String tableName = request.getParameter("newTableName");
			if (destiColTypes != null && !"".equalsIgnoreCase(destiColTypes)
					&& !"null".equalsIgnoreCase(destiColTypes)) {
				destiColObj = (JSONObject) JSONValue.parse(destiColTypes);
				if (destiColObj != null && !destiColObj.isEmpty()) {
					Set keys = destiColObj.keySet();
					Iterator itr = keys.iterator();
					int size = destiColObj.size();
					int i = 0;
					String createTableQuery = "CREATE TABLE " + tableName + "( ";
					while (itr.hasNext()) {
						String keyName = (String) itr.next();
						String keyValue = (String) destiColObj.get(keyName);
						if (i < (size - 1)) {
							createTableQuery += keyName + " " + keyValue + ",";
						} else {
							createTableQuery += keyName + " " + keyValue + ")";
						}
						i++;
					}
					int count = access.executeUpdateSQL(createTableQuery);
					resultObj.put("Message", "Table is created with " + tableName);
				}
			}
			if (connection != null) {
				connection.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			resultObj.put("Message", "Table is not created.");
		}
		return resultObj;
	}

	@Transactional
	public JSONObject insertMergeTablesData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			List selectColsList = new ArrayList();
			List insertColsList = new ArrayList();
			List totalDataList = new ArrayList();
			String fetchColumns = "";
			List columnsTypesList = new ArrayList();
			JSONObject columnsTypesObj = new JSONObject();
			JSONObject sourceDestiColsObj = new JSONObject();
			String sourceDestiCols = request.getParameter("sourceDestiCols");
			String destiColTypes = request.getParameter("destiColTypes");
			String tablesObj = request.getParameter("tablesObj");
			String tableName = request.getParameter("newTableName");
			String joinQueryVal = request.getParameter("joinQueryVal");
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {
				for (int j = 1; j <= columnCount; j++) {
					String columnName = metadata.getColumnName(j);
					String columnType = metadata.getColumnTypeName(j);
					columnsTypesObj.put(columnName, columnType);
				}
			}
			if (sourceDestiCols != null && !"".equalsIgnoreCase(sourceDestiCols)
					&& !"null".equalsIgnoreCase(sourceDestiCols)) {
				sourceDestiColsObj = (JSONObject) JSONValue.parse(sourceDestiCols);
			}
			if (sourceDestiColsObj != null && !sourceDestiColsObj.isEmpty()) {
				Set keys = sourceDestiColsObj.keySet();
				Iterator itr = keys.iterator();
				Object[] selectColsObj = new Object[keys.size()];
				Object[] insertColsObj = new Object[keys.size()];
				int i = 0;
				while (itr.hasNext()) {
					String keyName = (String) itr.next();
					String keyVal = (String) sourceDestiColsObj.get(keyName);
					columnsTypesList.add(columnsTypesObj.get(keyVal));
					selectColsObj[i] = keyName;
					insertColsObj[i] = keyVal;
					i++;
				}
				if (selectColsObj != null && selectColsObj.length > 0 && insertColsObj != null
						&& insertColsObj.length > 0) {
					selectColsList = Arrays.asList(selectColsObj);
					insertColsList = Arrays.asList(insertColsObj);
					selectColsList = (List) selectColsList.stream()
							.filter(e -> (e != null && !"".equalsIgnoreCase(String.valueOf(e)))).map(s -> {
								String str = ((String) s).replace(".", "_");
								s = s + " AS " + str;
								return s;
							}).collect(Collectors.toList());
					String delimiter = ",";
					if (selectColsList != null && !selectColsList.isEmpty()) {
						fetchColumns = String.join(delimiter, selectColsList);
					}
				}
			}
			if (fetchColumns != null && !"".equalsIgnoreCase(fetchColumns) && !"null".equalsIgnoreCase(fetchColumns)
					&& joinQueryVal != null && !"".equalsIgnoreCase(joinQueryVal)
					&& !"null".equalsIgnoreCase(joinQueryVal)) {
				int limit = 10000;
				int start = 0;
				String selectQuery = "SELECT DISTINCT " + fetchColumns + " " + joinQueryVal;
				System.out.println("select Query :::" + selectQuery);
				List dataList = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), limit, start);
				totalDataList.addAll(dataList);
				while (dataList.size() >= limit) {
					start = start + limit;
					dataList = access.sqlqueryWithParamsLimit(selectQuery, new HashMap(), limit, start);
					totalDataList.addAll(dataList);
					System.gc();
				}
				if (totalDataList != null && !totalDataList.isEmpty()) {
					resultObj = mergeTablesData(request, tableName, preparedStatement, insertColsList, totalDataList,
							insertColsList, columnsTypesList, "ORACLE", connection);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			resultObj.put("message", "0 records are Imported.");
		}
		return resultObj;
	}

	@Transactional
	public JSONObject mergeTablesData(HttpServletRequest request, String tableName, PreparedStatement preparedStatement,
			List headersList, List dataList, List insertColsList, List columnsTypesList, String dataBaseType,
			Connection connection) {
		JSONObject resultObj = new JSONObject();
		try {
			String columnsStr = (String) insertColsList.stream().map(e -> e).collect(Collectors.joining(","));
			String paramsStr = (String) insertColsList.stream().map(e -> "?").collect(Collectors.joining(","));
			String insertQuery = "INSERT INTO " + tableName + " (" + columnsStr + " ) VALUES ( " + paramsStr + ")";

			preparedStatement = connection.prepareStatement(insertQuery);
			int insertCount = insertDataIntoTable(request, tableName, preparedStatement, insertColsList, dataList,
					insertColsList, columnsTypesList, "ORACLE", null);
			String message = "Created table " + tableName + " and " + insertCount + " Records are Imported.";
			resultObj.put("message", message);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getScriptModalChartDataList(HttpServletRequest request) {
		JSONObject chartListObj = new JSONObject();
		try {
			String query = request.getParameter("script");
			if (query != null && !"".equalsIgnoreCase(query)) {
				String columnsKeys = request.getParameter("columnsKeys");
				JSONArray columnsKeysArr = new JSONArray();
				if (columnsKeys != null && !"".equalsIgnoreCase(columnsKeys)) {
					columnsKeysArr = (JSONArray) JSONValue.parse(columnsKeys);
				}
				int dataCount = 0;
				String countQuery = "SELECT COUNT(*) FROM(" + query + ")";
				if (countQuery != null && !"".equalsIgnoreCase(countQuery)) {
					List countData = access.sqlqueryWithParams(countQuery, new HashMap());
					if (countData != null && !countData.isEmpty()) {
						dataCount = countData.size();
						chartListObj.put("totalChartCount", dataCount);
					}
				}

				List selectData = access.sqlqueryWithParamsLimit(query, new HashMap(), 10, 0);
				if (selectData != null && !selectData.isEmpty()) {
					chartListObj.put("chartList", selectData);
				}
				if (columnsKeysArr != null && !columnsKeysArr.isEmpty()) {
					List listColData = (List) columnsKeysArr.stream()
							.filter(col -> (col != null && !"".equalsIgnoreCase(((String) col)))).map(column -> {
								String colKey = "";
								if (((String) column).contains(".")) {
									colKey = ((String) column).split("\\.")[1];
									colKey = colKey.replaceAll("\\)", "");
									if (colKey != null && !"".equalsIgnoreCase(colKey)
											&& !"null".equalsIgnoreCase(colKey)) {
										colKey = colKey.replaceAll("_", " ");
									}
									System.out.println("if:::" + colKey);
								} else {
									colKey = (String) column;
									colKey = colKey.replaceAll("\\)", "");
									if (colKey != null && !"".equalsIgnoreCase(colKey)
											&& !"null".equalsIgnoreCase(colKey)) {
										colKey = colKey.replaceAll("_", " ");
									}
									System.out.println("else:::" + colKey);
								}
								return colKey;
							}).collect(Collectors.toList());

					chartListObj.put("columnKeys", listColData);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chartListObj;
	}

	@Transactional
	public JSONObject getConvAIMergeTableColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		try {
			String tableDiv = "";
			JSONArray tablesArr = new JSONArray();
			String tablesStr = request.getParameter("tablesArr");
			if (tablesStr != null && !"".equalsIgnoreCase(tablesStr)) {
				tablesArr = (JSONArray) JSONValue.parse(tablesStr);
			}
			if (tablesArr != null && !tablesArr.isEmpty()) {
				tableDiv = "<div id='visionConvAIMergeTableColumnsParentId' class='visionConvAIMergeTableColumnsParentClass'>";
				for (int i = 0; i < tablesArr.size(); i++) {
					tableDiv += "<div id='visionConvAIMergeTableColumns" + tablesArr.get(i)
							+ "Id' class='visionConvAIMergeTableColumnsClass'>";
					tableDiv += "<div id='visionConversationalAIMergeTableColumns" + tablesArr.get(i)
							+ "Id' class='visionConvAIMergeTableColumnsNameClass'>" + tablesArr.get(i) + "</div>";
					Class.forName(dataBaseDriver);
					connection = DriverManager.getConnection(dbURL, userName, password);
					Statement statement = connection.createStatement();
					ResultSet results = statement.executeQuery("SELECT * FROM " + tablesArr.get(i) + "");
					ResultSetMetaData metadata = results.getMetaData();
					int columnCount = metadata.getColumnCount();
					if (columnCount > 0) {
						for (int j = 1; j <= columnCount; j++) {
							String columnName = metadata.getColumnName(j);
							String tableColName = tablesArr.get(i) + "." + columnName;
							String tableColumnName = tablesArr.get(i) + "_" + columnName;
							String colType = metadata.getColumnTypeName(j);
							if (colType != null && !"".equalsIgnoreCase(colType)
									&& "VARCHAR2".equalsIgnoreCase(colType)) {
								colType = colType + "(4000)";
							} else if (colType != null && !"".equalsIgnoreCase(colType)
									&& "CHAR".equalsIgnoreCase(colType)) {
								colType = colType + "(100)";
							}

							tableDiv += "<span class='convAIMergeSelectTableColumnsSpanClass'><label for=\""
									+ tableColumnName + "\"><input type=\"checkbox\" data-tablecolName=\""
									+ tableColName + "\" data-colType=\"" + colType + "\" id=\"" + tableColumnName
									+ "\" name=\"mergeTablesColumns\" value=\"" + columnName + "\">" + columnName
									+ "</label></span>";
						}
					}
					tableDiv += "</div>";
				}
				tableDiv += "</div>";
			}
			resultObj.put("tableDiv", tableDiv);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public JSONObject getEditorViewUserTableNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String tableDiv = "";
			String userName = (String) request.getParameter("userName");
			String editorFlag = request.getParameter("editorFlag");
			if (userName != null && !"".equalsIgnoreCase(userName)) {
				String fetchQuery = "SELECT TABLE_NAME  FROM C_ETL_DAL_AUTHORIZATION WHERE CREATE_BY =:CREATE_BY";
				Map mapData = new HashMap();
				mapData.put("CREATE_BY", userName);
				List listData = access.sqlqueryWithParams(fetchQuery, mapData);
				if (listData != null && !listData.isEmpty()) {
					tableDiv = "<div id='editorViewUserTableNamesDivId' class='editorViewUserTableNamesDivClass'>"
							+ "<p class='editorExistTablesHeaderClass'>Existing Files/Tables</p>"
							+ "<div class=\"search\">"
							+ "<input type=\"text\" placeholder=\"search\" id='table-search'/>" + "</div>"
							+ "<div id='userEditorViewTableNamesDivId' class='userEditorViewTableNamesDivClass'>";
					for (int i = 0; i < listData.size(); i++) {
						String tableName = (String) listData.get(i);
						tableDiv += "<div id='" + tableName
								+ "_table' class='userTableNameClass' onclick=getEditorViewTableColumns('" + tableName
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
	public JSONObject getEditorViewTableColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String columnDiv = "";
			String tableName = request.getParameter("tableName");
			if (tableName != null && !"".equalsIgnoreCase(tableName)) {
				String columnQuery = "SELECT COLUMN_NAME  TABLE_NAME  FROM  USER_TAB_COLUMNS WHERE TABLE_NAME=:TABLE_NAME";
				Map columnMap = new HashMap();
				columnMap.put("TABLE_NAME", tableName);
				List listData = access.sqlqueryWithParams(columnQuery, columnMap);
				if (listData != null && !listData.isEmpty()) {
					columnDiv = "<p class='editorExistTablesHeaderClass'>" + tableName + " Columns</p>"
							+ "<div class=\"search\">"
							+ "<input type=\"text\" placeholder=\"search\" id='column_search'/>" + "</div>"
							+ "<div id='userColumnNamesDivId' class='userColumnNamesDivClass text-left'>";

					for (int i = 0; i < listData.size(); i++) {
						String columnName = (String) listData.get(i);
						columnDiv += "<div id='" + columnName
								+ "_column' class='userColumnNameClass' data-intelliSenseViewColumnfilter-item data-filter-name=\""
								+ columnName + "\">" + columnName + "</div>";

					}
					columnDiv += "</div>";
					resultObj.put("columnDiv", columnDiv);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@Transactional
	public String updateCardData(HttpServletRequest request) {
		String updateStatus = "";
		try {
			String cardData = request.getParameter("cardData");
			String cardId = request.getParameter("cardId");
			if (!dashboardutils.isNullOrEmpty(cardData)) {
				JSONObject cardDataObj = (JSONObject) JSONValue.parse(cardData);
				String title = (String) cardDataObj.get("title");
				String trend = (String) cardDataObj.get("trend");
				String cardDateParamObj = (String) cardDataObj.get("cardDateParamObj");
				String valueStr = (String) cardDataObj.get("valueStr");
				String UpdateQuery = "UPDATE O_RECORD_VISUALIZATION SET Y_AXIS_VALUE = ?, CHART_TITTLE = ?, VISUALIZE_CUST_COL14 =?, VISUALIZE_CUST_COL18 =?  WHERE CHART_ID =?";
				Map<Integer, Object> updateMap = new HashMap<>();
				updateMap.put(1, valueStr);
				updateMap.put(2, title);
				updateMap.put(3, cardDateParamObj);
				updateMap.put(4, trend);
				updateMap.put(5, cardId); // chartId 
				System.out.println("updateMap:::" + updateMap);
				int updatecount = access.executeNativeUpdateSQLWithSimpleParams(UpdateQuery, updateMap);
				if (updatecount != 0) {
					updateStatus = "Card updated successfully.";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return updateStatus;
	}

	@Transactional
	public String deleteDashboard(HttpServletRequest request) {
		String deleteResult = "";
		try {
			String condition = "";
			JSONArray dashboardList = null;
			String dashboardLisrStr = request.getParameter("dashboardList");
			if (!dashboardutils.isNullOrEmpty(dashboardLisrStr)) {
				dashboardList = (JSONArray) JSONValue.parse(dashboardLisrStr);
				condition = (String) dashboardList.stream().collect(Collectors.joining("','", "'", "'"));
			}
			String query = "DELETE FROM O_RECORD_VISUALIZATION WHERE DASHBORD_NAME IN (" + condition + ")";
			Integer deleteCount = access.executeUpdateSQLNoAudit(query, Collections.EMPTY_MAP);
			if (deleteCount != null && deleteCount > 0) {
				deleteResult = "Deleted Successfully.";
			} else {
				deleteResult = "Unable to delete Dashboard(s).";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return deleteResult;
	}

	public String getOracleDataTypeOfValue(String value, int length) {
		String dataType = "";
		try {
			if (value != null) {
				if (isNumeric(value)) {
					dataType = "NUMBER";
				} else if (isValidDate(value)) {
					dataType = "DATE";
				} else if (isBooleanValue(value)) {
					dataType = "BOOLEAN";
				} else if (isCharacter(value)) {
					dataType = "VARCHAR2(4)";
				} else {
					dataType = "VARCHAR2(" + (length + 100) + ")";
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataType;
	}

	public boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		try {
			strNum = strNum.replace(",", "");
			double d = Double.parseDouble(strNum);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public boolean isValidDate(String dateStr) {
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        sdf.setLenient(false);
		try {
			sdf.parse(dateStr);
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

	public boolean isBooleanValue(String str) {
		if (str == null) {
			return false;
		}
		if ("TRUE".equalsIgnoreCase(str) || "FALSE".equalsIgnoreCase(str)) {
			return true;
		} else {
			return false;
		}

	}

	public boolean isCharacter(String str) {
		if (str == null) {
			return false;
		}
		if (str.length() == 1) {
			return true;
		} else {
			return false;
		}

	}
	
	@Transactional
	public JSONObject getEditDashBoardNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String tableDiv = "";
			String userName = (String) request.getParameter("userName");
			if (userName != null && !"".equalsIgnoreCase(userName)) {
				String roleId = (String) request.getSession(false).getAttribute("ssRole");
				String subQuery = "";
				Map dashBoardMap =new HashMap();
				if (roleId != null && !"".equalsIgnoreCase(roleId) && !"".equalsIgnoreCase(roleId)
						&& (roleId.contains("REQUESTOR") || roleId.contains("APPROVER"))) {
					subQuery = " AND CREATE_BY =:CREATE_BY ";
					dashBoardMap.put("CREATE_BY", userName);
				}
				String dashBoardQuery = "SELECT DISTINCT DASHBORD_NAME FROM O_RECORD_VISUALIZATION WHERE ROLE_ID =:ROLE_ID "
						+ subQuery + " ORDER BY DASHBORD_NAME";
				JSONArray dashBordArr = new JSONArray();
				dashBoardMap.put("ROLE_ID", roleId);
				List dashBoardList = access.sqlqueryWithParams(dashBoardQuery, dashBoardMap);
				if (dashBoardList != null && !dashBoardList.isEmpty()) {
					tableDiv = "<div id='userDashBoardNamesDivId' class='userDashBoardNamesDivClass'>"
							+ "<div class=\"search\">"
							+ "<input type=\"text\" placeholder=\"search\" id='dashBoard-names-search'/>" + "</div>"
							+ "<ul id='userIntellisenseViewDashBoardNamesDivId' class='userIntellisenseViewDashBoardNamesDivClass'>";
					for (int i = 0; i < dashBoardList.size(); i++) {
						String dashBoardName = (String) dashBoardList.get(i);
						if(dashBoardName !=null && !"".equalsIgnoreCase(dashBoardName) && !"null".equalsIgnoreCase(dashBoardName))
						{
							String dashBoardVal =dashBoardName.replaceAll(" ", "_");
							tableDiv += "<li id='" + dashBoardVal
									+ "_table' class='userDashBoardNameClass'"
									+ "data-intelliSenseViewTablefilter-item data-filter-name='"+dashBoardName.toUpperCase()+"'><span contenteditable=\"true\">" + dashBoardName + "</span>"
								    + "<input type='button' value='Save' onclick=\"saveDashBoardName('"+dashBoardName+"','"+dashBoardVal+"_table')\"/>"
									+ "</li>";	
						}
						
					}
					tableDiv += "</ul>" + "</div>";
				}
				resultObj.put("tableDiv", tableDiv);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	
	@Transactional
	public JSONObject getSaveDashBoardNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String dashBoardName = (String) request.getParameter("dashBoardName");
			String newDashBoardName = (String) request.getParameter("newDashBoardName");
			if (userName != null && !"".equalsIgnoreCase(userName)) {
				String roleId = (String) request.getSession(false).getAttribute("ssRole");
				Map updateMap =new HashMap();
				String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET DASHBORD_NAME='"+newDashBoardName+"' WHERE ROLE_ID =:ROLE_ID AND "
						  + "DASHBORD_NAME=:DASHBORD_NAME";
				JSONArray dashBordArr = new JSONArray();
				updateMap.put("ROLE_ID", roleId);
				updateMap.put("DASHBORD_NAME", dashBoardName);
				int count = access.executeUpdateSQL(updateQuery, updateMap);
				if (count > 0) {
					resultObj.put("message", newDashBoardName+" Updated Successfully.");  
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	
	@Transactional
	public JSONObject getChartNotes(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String chartId = (String) request.getParameter("chartId");
			if (chartId != null && !"".equalsIgnoreCase(chartId)) {
				String roleId = (String) request.getSession(false).getAttribute("ssRole");
				Map selectMap =new HashMap();
				String selectQuery = "SELECT VISUALIZE_CUST_COL16 FROM O_RECORD_VISUALIZATION WHERE ROLE_ID =:ROLE_ID AND "
						  + "CHART_ID=:CHART_ID";
				JSONArray dashBordArr = new JSONArray();
				selectMap.put("ROLE_ID", roleId);
				selectMap.put("CHART_ID", chartId);
				List  listData = access.sqlqueryWithParams(selectQuery, selectMap);
				if (listData !=null && !listData.isEmpty()) {
					String notes  = (String)new PilogUtilities().clobToString((Clob)listData.get(0)); 
					resultObj.put("notes", notes); 
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	
	@Transactional
	public JSONObject saveChartNotes(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String chartId = (String) request.getParameter("chartId");
			String notes = (String) request.getParameter("chartNotes");
			if (notes != null && !"".equalsIgnoreCase(notes)) {
				String roleId = (String) request.getSession(false).getAttribute("ssRole");
				Map updateMap =new HashMap();
				String updateQuery = "UPDATE O_RECORD_VISUALIZATION SET VISUALIZE_CUST_COL16='"+notes+"' WHERE ROLE_ID =:ROLE_ID AND "
						  + "CHART_ID=:CHART_ID";
				JSONArray dashBordArr = new JSONArray();
				updateMap.put("ROLE_ID", roleId);
				updateMap.put("CHART_ID", chartId);
				int count = access.executeUpdateSQL(updateQuery, updateMap);
				if (count > 0) {
					resultObj.put("message", "Notes Updated Successfully.");  
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	
	@Transactional
	  public List getLgFeature(HttpServletRequest request)
	  {
	    List datalist = new ArrayList();
	    try
	    {
	      String selectQuery = "SELECT SUB_FEATURE,(CASE WHEN TRIM (BASIC_LG) IS NULL THEN CAST(' ' AS varchar2(10 CHAR)) ELSE BASIC_LG END) AS BASIC_LG,(CASE WHEN TRIM (STANDARD_LG) IS NULL THEN CAST(' ' AS varchar2(10 CHAR)) ELSE STANDARD_LG END) AS STANDARD_LG,(CASE WHEN TRIM (ADVANCED_LG) IS NULL THEN CAST(' ' AS varchar2(10 CHAR)) ELSE ADVANCED_LG END) AS ADVANCED_LG,PRICE,PRICE_FLAG,"
                  + "(CASE WHEN TRIM (ICON) IS NULL THEN CAST(' ' AS varchar2(10 CHAR)) ELSE ICON END) AS ICON  FROM DAL_IG_FEATURES WHERE ACTIVE_FLAG = 'Y' Order by SEQ_NUMBER ASC ";
	      
	      datalist = access.sqlqueryWithParams(selectQuery, Collections.EMPTY_MAP);
	    }
	    catch (Exception e)
	    {
	      e.printStackTrace();
	    }
	    return datalist;
	  }
	  
	  @Transactional
	  public List getSubFeature(HttpServletRequest request, String featureName)
	  {
	    List datalist = new ArrayList();
	    try
	    {
	      String selectQuery = "SELECT SUB_FEATURE,BASIC_LG,STANDARD_LG,ADVANCED_LG, (CASE WHEN TRIM (ICON_PATH) IS NULL THEN CAST(' ' AS varchar2(10 CHAR)) ELSE ICON_PATH END) AS ICON_PATH FROM DAL_IG_FEATURES  where FEATURE =:FEATURE AND ACTIVE_FLAG = 'Y' Order by SEQ_NUMBER ASC";
	      HashMap datamap = new HashMap();
	      datamap.put("FEATURE", featureName);
	      datalist = access.sqlqueryWithParams(selectQuery, datamap);
	    }
	    catch (Exception e)
	    {
	      e.printStackTrace();
	    }
	    return datalist;
	  } 
	  
	  
	  
	  @Transactional
	    public List getInfoDynamicHTML(HttpServletRequest request) {
	        StringBuilder result = new StringBuilder();
	        List resultList = new ArrayList();
	        try {
	            String query = "SELECT COL_NAME, COL_LABEL, FIELD_TYPE, COND_FLAG, ATTR_VALUE,COL_FLTR_TYPE FROM DAL_GRID_ROLE_COL_LINK WHERE GRID_ID='IG_CUSTOMER_INFO' ORDER BY COL_SEQ ASC";
	            resultList = access.sqlqueryWithParams(query, Collections.EMPTY_MAP);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultList;
	    }

	    public StringBuilder getInfoBasicPlan(HttpServletRequest request, String lgType) {
	        StringBuilder sideLgfeatures = new StringBuilder();
	        try {
	            lgType = lgType.toUpperCase();
	            String hiLevelquery = "SELECT DISTINCT LG_HIGH_LEVEL_FEATURE FROM (SELECT LG_HIGH_LEVEL_FEATURE "
	                    + " FROM IG_REGION_PRICE_DETAILS WHERE REGION = :REGION AND COUNTRY_CODE = :COUNTRY_CODE"
	                    + " AND LG_MODEL = :LG_MODEL AND ACTIVE_FLAG = 'Y' ORDER BY SL_NO ASC) ";
	            Map hiLevelqueryMap = new HashMap<>();
	            hiLevelqueryMap.put("REGION", lgType);
	            hiLevelqueryMap.put("COUNTRY_CODE", lgType);
	            hiLevelqueryMap.put("LG_MODEL", lgType);     
	            List hiLevelList = access.sqlqueryWithParams(hiLevelquery, hiLevelqueryMap);

	            if (hiLevelList != null && !hiLevelList.isEmpty()) {
	                for (int i = 0; i < hiLevelList.size(); i++) {
	                    String hihlevelTitle = (String) hiLevelList.get(i);
	                    sideLgfeatures.append("<div class=\"planSection\"><span id=\"selectedTittlePlanInclude\" style=\"display:none\">" + lgType + "</span><div class=\"plansAccordian\"><span id=\"selectedTittlePlanInclude\">" + hihlevelTitle + "</span></div>");
	                    String subFeaturesquery = "SELECT LG_SUB_FEATURE FROM IG_REGION_PRICE_DETAILS "
	                            + " WHERE REGION = :REGION AND COUNTRY_CODE = :COUNTRY_CODE "
	                            + " AND LG_MODEL = :LG_MODEL AND ACTIVE_FLAG = 'Y' "
	                            + " AND LG_HIGH_LEVEL_FEATURE = :LG_HIGH_LEVEL_FEATURE ORDER BY SL_NO ASC";

	                    Map subFeaturesMap = new HashMap<>();
	                    subFeaturesMap.put("REGION", lgType);
	                    subFeaturesMap.put("COUNTRY_CODE", lgType);
	                    subFeaturesMap.put("LG_MODEL", lgType);
	                    subFeaturesMap.put("LG_HIGH_LEVEL_FEATURE", hihlevelTitle);
	                    List subFeaturesMapList = access.sqlqueryWithParams(subFeaturesquery, subFeaturesMap);

	                    if (subFeaturesMapList != null && !subFeaturesMapList.isEmpty()) {
	                        sideLgfeatures.append("<ul class=\"plansList\" id=\"plansCollapse\">");
	                        for (int j = 0; j < subFeaturesMapList.size(); j++) {
	                            String subFeaturesTitle = (String) subFeaturesMapList.get(j);
	                            sideLgfeatures.append(" <li><i class=\"fa fa-check\" aria-hidden=\"true\"></i><span>" + subFeaturesTitle + "</span></li>");
	                        }
	                        sideLgfeatures.append("</ul></div>");
	                    }

	                }
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return sideLgfeatures;
	    }
	    
	    @Transactional(propagation = Propagation.REQUIRES_NEW)
	    public JSONObject getCountryList(HttpServletRequest request) {
	        JSONObject resultobj = new JSONObject();
	        List dataList = new ArrayList();
	        List cityList = new ArrayList();
	        List stateList = new ArrayList();
	        try {
	            String coutryQuery = "select STD_CODE,DESCRIPTION from B_COUNTRY ORDER BY DESCRIPTION ASC";
	            dataList = access.sqlqueryWithParams(coutryQuery, Collections.EMPTY_MAP);
	            String cityQuery = "select DISTINCT CITY from B_CITY ORDER BY CITY ASC";
	            cityList = access.sqlqueryWithParams(cityQuery, Collections.EMPTY_MAP);
	            String stateQuery = "select DISTINCT STATE from B_CITY ORDER BY STATE ASC";
	            stateList = access.sqlqueryWithParams(stateQuery, Collections.EMPTY_MAP);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        resultobj.put("dataList", dataList);
	        resultobj.put("cityList", cityList);
	        resultobj.put("stateList", stateList);
	        return resultobj;
	    }
	    
	    @Transactional
	    public List getFeaturesPrice()
	    {
	    	List listData = new ArrayList();
	    	try {
	    		 String processGstUrlQuery = "SELECT GRID_INIT_PARAMS FROM DAL_GRID_ROLE_LINK WHERE GRID_ID = 'IGFEATURESPRICE' AND ROLE_ID = 'IGFEATURESPRICE'";
	                HashMap<String, Object> processgstMap = new HashMap<String, Object>();
	                listData = access.sqlqueryWithParams(processGstUrlQuery, processgstMap);
	    	}catch(Exception ex)
	    	{
	    		ex.printStackTrace();
	    	}
	    	return listData;
	    }
	    
	    
	    @Transactional
	    public String getstate(HttpServletRequest request) {
	        List statelist = new ArrayList();
	        String result = "";
	        String countrycode = "";

	        try {
	            String Country = request.getParameter("country");
	            String countryquery = "SELECT DISTINCT LAND1 FROM V_COUNTRY WHERE DESCRIPTION = '" + Country + "' ";
	            List countrylist = access.sqlqueryWithParams(countryquery, Collections.EMPTY_MAP);
	            if (countrylist != null && !countrylist.isEmpty()) {
	                for (int i = 0; i < countrylist.size(); i++) {
	                    countrycode = (String) countrylist.get(i);
	                }
	            }
	            String stateQuery = "SELECT DISTINCT STATE FROM V_STATES WHERE UPPER(COUNTRY_NAME) = '" + Country.toUpperCase() + "' ORDER BY STATE ASC ";
	            statelist = access.sqlqueryWithParams(stateQuery, Collections.EMPTY_MAP);
	            result += "<option >Select State</option>\n";
	            if (statelist != null && !statelist.isEmpty()) {
	                for (int i = 0; i < statelist.size(); i++) {
	                    String statesList = (String) statelist.get(i);
	                    result += "<option >" + statesList + "</option>\n";
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;

	    }
	    public List getCity(HttpServletRequest request) {
	        List resultList = new ArrayList();
	        try {
	            String state = (String) request.getParameter("state");
	            String country = (String) request.getParameter("country");
	            String stateQuery = "SELECT DISTINCT CITY FROM V_CITY WHERE Upper(STATE) = '" + state.toUpperCase() + "' AND upper(COUNTRY_NAME) = '" + country.toUpperCase() + "' ORDER BY CITY ASC ";
	            resultList = access.sqlqueryWithParams(stateQuery, Collections.EMPTY_MAP);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultList;
	    }

	    public JSONObject addOnpackage(HttpServletRequest request) {
	        String result = "";
	        JSONArray featureArr = new JSONArray();
	        JSONObject resultobj = new JSONObject();
	        try {
	            
	             JSONObject textParam = new JSONObject();
	            Enumeration enumeration = request.getParameterNames();
	            while (enumeration.hasMoreElements()) {
	                String name = enumeration.nextElement().toString();
	                String val = (String) request.getParameter(name);
	                textParam.put(name, val);
	            }
	            String text = "Pending";
	            request.getSession(false).setAttribute("flag", "P");
	            String orgn_id = saveTransactionDetailsDB(request, textParam, "");
	             String encodedString = Base64.getEncoder().encodeToString(orgn_id.getBytes());
	            
	            DecimalFormat df = new DecimalFormat("#,###");
	            int i = 1;
	            StringBuilder resultStr = new StringBuilder();
	            String totalAmount = "";
	            String currency = "";
	            String lgType = (String) request.getParameter("tittle");
	            String country = (String) request.getParameter("billing_country");
	            String countryquery = "SELECT LAND1 FROM V_COUNTRY WHERE DESCRIPTION = '" + country + "' ";
	            List countrylist = access.sqlqueryWithParams(countryquery, Collections.EMPTY_MAP);
	            if (countrylist != null && !countrylist.isEmpty()) {
	                String countrycode = (String) countrylist.get(0);
	                resultStr.append("<div id=\"subcriptionAddOnsAndDiscount\" class=\"container-fluid\">")
	                        .append("<div class=\"billingInfoInnerContainer d-flex flex-column\">");
	                String query = "SELECT LG_HIGH_LEVEL_FEATURE, LG_SUB_FEATURE, PRICE, DISCOUNT_NAME,  DISCOUNT_PCT, ACTIVE_FLAG, INCLUDE_FLAG, CURRENCY FROM IG_REGION_PRICE_DETAILS WHERE"
	                        + " LG_MODEL =:LG_MODEL  "
	                        //                        + "AND COUNTRY_NAME =:COUNTRY_NAME "
	                        + "AND COUNTRY_CODE =:COUNTRY_CODE AND LG_HIGH_LEVEL_FLAG ='Y' AND ACTIVE_FLAG = 'Y' ORDER BY SL_NO ASC";
	                HashMap updatemap = new HashMap();
//	                updatemap.put("COUNTRY_NAME", country.toUpperCase());
	                updatemap.put("COUNTRY_CODE", countrycode.toUpperCase());
	                updatemap.put("LG_MODEL", lgType.toUpperCase());
	                List resultList = access.sqlqueryWithParams(query, updatemap);
	                if (resultList != null && !resultList.isEmpty()) {
	                    for (Object dataObj : resultList) {
	                        Object[] dataArrObj = (Object[]) dataObj;
	                        String highLevelFet = (String) dataArrObj[0];
	                        updatemap.put("LG_HIGH_LEVEL_FEATURE", highLevelFet);
	                        String subFeatureHeading = (String) dataArrObj[1];
	                        if (i == 1) {
	                            String amountData = (String) dataArrObj[2];
	                            String dis = (String) dataArrObj[4];
	                            int amount = parseInt(amountData) * parseInt(dis) / 100;
	                            int finalAmount = parseInt(amountData) - amount;
	                            totalAmount = String.valueOf(finalAmount);
//	                            resultStr.append("<div class=\"subscriptionPlansIncludes\">")
//	                                    .append("<table class=\"table-bordered table table-hover features\">")
//	                                    .append("<thead><tr  data-toggle=\"collapse\" id=\"mainparentClass\"  class=\"555\"  aria-expanded=\"true\">")
//	                                    .append("<td colspan=\"4\"><div class=\"subFeatureTextDiv\"><span class=\"subFeatureTextSpan\"><b>" + subFeatureHeading + "</b></span><span class=\"subFeatureText\">" + dataArrObj[7] + "</span><del class=\"subFeatureAmount\">" + currencySymbol(dataArrObj[7].toString()) + df.format(parseInt(dataArrObj[2].toString())) + "</del></div></td>")
//	                                    .append("<td style=\"text-align: center;\"><span class=\"subFeatureCurrencySpan\">" + currencySymbol(dataArrObj[7].toString()) + "</span><span class=\"subFeatureAmountDis\">" + df.format(parseInt(totalAmount)) + "</span><span class=\"totalPerDesCol\">(" + dis + "%)</span></td>")
//	                                    .append("</tr></thead><tbody>")
//	                                    .append("<tr data-toggle=\"collapse\" data-target=\"#accordionFeatures\">")
//	                                    .append("<td colspan=\"5\"  style=\"padding-left:28px; text-align: left !important;\" class=\"active\"><b class=\"subTitleClass\">Features Included</b></td>")
//	                                    .append("</tr>");
//	                            
	                            resultStr.append("<div class=\"subscriptionPlansIncludes\">")
	                                    .append("<table class=\"table-bordered table table-hover features\">")
	                                    .append("<thead><tr  data-toggle=\"collapse\" id=\"mainparentClass\"  class=\"555\"  aria-expanded=\"true\">")
	                                    .append("<td colspan=\"4\"><div class=\"subFeatureTextDiv\"><span class=\"subFeatureTextSpan\"><b>" + subFeatureHeading + "</b></span><span class=\"subFeatureText\">" + dataArrObj[7] + "</span><span class=\"subFeatureAmount\"><del>" + currencySymbol(dataArrObj[7].toString()) + "</del><del id=\"OriginalAnnualFee\">" + df.format(parseInt(dataArrObj[2].toString())) + "</del></span></div></td>") //som
	                                    .append("<td style=\"text-align: center;\"><span class=\"subFeatureCurrencySpan\">" + currencySymbol(dataArrObj[7].toString()) + "</span><span class=\"subFeatureAmountDis\" id=\"DisAnnualFee\">" + df.format(parseInt(totalAmount)) + "</span><span class=\"totalPerDesCol\">(" + dis + "%)</span></td>")
	                                    .append("</tr></thead><tbody>")
	                                    .append("<tr data-toggle=\"collapse\" data-target=\"#accordionFeatures\">")
	                                    .append("<td colspan=\"5\"  style=\"padding-left:28px; text-align: left !important;\" class=\"active\"><b class=\"subTitleClass\">Features Included</b></td>")
	                                    .append("</tr>");
	                            
	                            currency = (String) dataArrObj[7];
	                        } else if(i == 2) {
	                            resultStr.append("<div class=\"recommendedAddonContainer\"><table class=\"table-bordered table table-hover recommended\"><thead>")
	                                    .append("<tr data-toggle=\"collapse\" id=\"mainparentClass\"  class=\"555\"  aria-expanded=\"true\">")
	                                    .append("<td colspan=\"5\"><b>Recommended Add-on(s)</b></td>")
	                                    .append("</tr></thead><tbody>");
	                        }
	                        String query2 = "SELECT LG_SUB_FEATURE, PRICE, DISCOUNT_NAME,  DISCOUNT_PCT, ACTIVE_FLAG, INCLUDE_FLAG, CURRENCY FROM IG_REGION_PRICE_DETAILS WHERE"
	                                + " LG_MODEL =:LG_MODEL  "
	                                //                                + "AND COUNTRY_NAME =:COUNTRY_NAME "
	                                + "AND COUNTRY_CODE =:COUNTRY_CODE AND LG_HIGH_LEVEL_FLAG ='N' AND LG_HIGH_LEVEL_FEATURE =:LG_HIGH_LEVEL_FEATURE AND ACTIVE_FLAG = 'Y'";
	                        List resultList2 = access.sqlqueryWithParams(query2, updatemap);
	                        if (resultList2 != null && !resultList2.isEmpty()) {
	                            for (Object dataObj2 : resultList2) {
	                                Object[] dataArrObj2 = (Object[]) dataObj2;
	                                String checkBoxFlag = (String) dataArrObj2[5];
	                                if (i == 1) {
	                                    resultStr.append("<tr id=\"accordionFeatures\"  class=\"collapse\" style=\"\">")
	                                            .append("<td style=\"padding-left:54px;\" colspan=\"4\">" + dataArrObj2[0] + "</td>");
	                                    if (checkBoxFlag.equalsIgnoreCase("Y")) {
	                                        resultStr.append("<td style=\"text-align: center;\"><input type=\"checkbox\" name=\"\" id=\"\" checked disabled></td>");
	                                    } else {
	                                        resultStr.append("<td style=\"text-align: center;\"><input type=\"checkbox\" name=\"\" id=\"\"></td>");
	                                    }
	                                    resultStr.append("</tr>");
	                                } else {
	                                    String amountData1 = (String) dataArrObj2[1];
	                                    String dis1 = (String) dataArrObj2[3];
	                                    int amount1 = parseInt(amountData1) * parseInt(dis1) / 100;
	                                    int finalAmount1 = parseInt(amountData1) - amount1;
	                                    resultStr.append("<tr id=\"recommendFeatures\"  class=\"\" style=\"\">")
	                                            .append("<td style=\"padding-left:28px;\" colspan=\"4\">" + dataArrObj2[0] + "</td>");
	                                    if (checkBoxFlag.equalsIgnoreCase("Y")) {
	                                        resultStr.append("<td style=\"text-align: center;\"><input type=\"checkbox\" onclick=\"setTotalAmount()\" name=\"\" id=\"\" checked></td>");
	                                    } else {
	                                        resultStr.append("<td style=\"text-align: center;\"><input type=\"checkbox\" class=\"chechBoxPaymentRecommend\" onclick=\"setTotalAmount()\" name=\"\" id=\"\"></td>");
	                                    }
	                                    resultStr.append("<td style=\"text-align:center\"><del>" + currencySymbol(dataArrObj2[6].toString()) + df.format(parseInt(dataArrObj2[1].toString())) + "  </del><span class=\"totalAmountCol\">&nbsp;&nbsp;" + currencySymbol(dataArrObj2[6].toString()) + "</span><span class=\"totalAmountCol\">" + df.format(finalAmount1) + " </span><span class=\"totalPerDesCol\">(" + dataArrObj2[3] + "%)</span></td></tr>");
	                                }
	                            }
	                        }
	                        resultStr.append("</tbody></table></div>");
	                        i++;
	                    }
	                }
	                resultStr.append("<div class=\"payment-Coupon-Code-Main-Div\">")
	                        .append("<div class=\"payment-coupon-code-Div\" id=\"payment-coupon-code-Div-ID\"><div class=\"form-group payment-dis-main-div\">")
	                        .append("<div class=\"payment-dis-level-div\"><label>Discount Code</label></div>")
	                        .append("<div class=\"payment-dis-input-div\"><input type=\"text\" id=\"coupon-Code-Input-payment-page\" class=\"coupon discount-code-ip\" name=\"\" placeholder=\"Discount code\">")
	                        .append("<button class=\"btn-coupon\" onclick=\"applyDisCodeProcess()\" >Apply</button><small class=\"error-msg-dis\" id=\"disErrorMsg\" style=\"display:none;\"></small></div>")
	                        .append("<div class=\"payment-dis-text-div\"><span class=\"payment-dis-text-span\" id=\"payment-dis-text-dis\" >")
	                        //                        .append("<span class=\"currencyType\">" + currencySymbol(currency) + "</span><del class=\"payment-dis-text-del\">" + df.format(parseInt(totalAmount)) + "</del>")
	                        .append("<span class=\"payment-dis-negetive-span\">-</span><span class=\"currencyType\">" + currencySymbol(currency) + "</span>")
	                        .append("<span class=\"payment-dis-text-first\" id=\"disCouponAmmount\">0</span><span class=\"payment-dis-text-second payment-dis-text-closeBr\">(</span>")
	                        .append("<span class=\"payment-dis-text-second\" id=\"disCouponPercentage\">0</span><span class=\"payment-dis-text-second\">%)</span></span><div>")
	                        .append("</div></div></div>");
	                resultStr.append("<div class=\"subscriptionTotalAmounCls\">")
	                        .append("<table class=\"table-bordered table table-hover mainparentTotalAmounCls\">")
	                        .append("<tbody><tr  id=\"mainparentTotalAmoun\">")
	                        .append("<td colspan=\"4\" style=\"text-align: center;width: 50%;\"><b>Total</b></td>")
	                        .append("<td style=\"width: 25%;text-align: center;\">" + currency + "</td>")
	                        .append("<td style=\"text-align: center;\"><span style=\"display:none;\" id=\"subscriptionOrginalDiscountTotalAmount\">" + df.format(parseInt(totalAmount)) + "</span><span class=\"subscriptionTotalAmount\">" + currencySymbol(currency) + "</span><span class=\"subscriptionTotalAmount\" id=\"subscriptionTotalAmount\">" + df.format(parseInt(totalAmount)) + "</span></td>")
	                        .append("</tr></tbody></table></div>");
	                resultStr.append("</div>");
	                resultStr.append("<div class=\"processPaymentDivCLS\"><button class=\"btn btn-primary\"  onclick=\"getprivious()\">Previous</button>")
	                        .append("<button type=\"submit\" class=\"btn btn-primary\" id=\"nbtn\" onclick=\"cartPaymentProcess('Basic',event)\">Proceed To Payment</button>")
	                        .append("<div></div>");
	                resultobj.put("result", resultStr);
	                resultobj.put("status", "Y");
	                resultobj.put("requestId", encodedString);
	            } else {
	                resultobj.put("status", "N");                
	                resultobj.put("status", "country Code not there");
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultobj;

	    }
	    
	    public String currencySymbol(String type) {
	        String result = "";
	        try {
	            if (type.equalsIgnoreCase("INR")) {
	                result = "₹";
	            } else if (type.equalsIgnoreCase("USD")) {
	                result = "$";
	            } else if (type.equalsIgnoreCase("EURO")) {
	                result = "€";
	            } else {
	                result = "$";
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	    
	    public String saveTransactionDetailsDB(HttpServletRequest request, JSONObject textParm, String text) {
	        String result = "";
	        String requestId = "";
	        try {
	            String requestIdData = textParm.get("requestId").toString();
	         
	            if (text != null && !"".equalsIgnoreCase(text)
	                         && !"null".equalsIgnoreCase(text) && "PayementCompleted".equalsIgnoreCase(text)) {
	            }
	            else if (requestIdData != null && !"".equalsIgnoreCase(requestIdData)
	                         && !"null".equalsIgnoreCase(requestIdData)) {
	                request.getSession(false).setAttribute("flag", "S");
	            }else
	            {
	             request.getSession(false).setAttribute("flag", "P");   
	            }
	            
	          if (requestIdData != null && !"".equalsIgnoreCase(requestIdData)
	                         && !"null".equalsIgnoreCase(requestIdData)) {
	              byte[] requestIdDataBytes = Base64.getDecoder().decode(requestIdData);
	                requestId = new String(requestIdDataBytes);
	            }
	            
	            String flag = (String) request.getSession(false).getAttribute("flag");
	            if (flag.equalsIgnoreCase("P")) {
	                String orgn_id = "";
	                StringBuilder clobText = new StringBuilder();
	                for (Object key : textParm.keySet()) {
	                    Object value = textParm.get(key);
	                    clobText.append("Key: " + key + ", Value: " + value + ",");
	                }
	                String ordIdQuery = "SELECT RAWTOHEX(SYS_GUID()) FROM DUAL";
	                List ordIdlist = access.sqlqueryWithParams(ordIdQuery, Collections.EMPTY_MAP);
	                if (ordIdlist != null && !ordIdlist.isEmpty()) {
	                    orgn_id = (String) ordIdlist.get(0);
	                } else {
	                    byte[] resBuf = new byte[16];
	                    new Random().nextBytes(resBuf);
	                    orgn_id = new String(Hex.encode(resBuf));
	                }
	                requestId = orgn_id;
//	                String insertQuery = "INSERT INTO DAL_LG_CONFIG(ORGN_ID, ORGN_NAME, CUSTOMER_CITY, CUSTOMER_STATE,"
//	                        + " CUSTOMER_COUNTRY, CONTACT_NAME, CONTACT_PHONE_NO, CONTACT_MAIL_ID,"
//	                        + " LG_MODEL_TYPE, DEFAULT_LG_MODEL_ORGN_ID, DOMAINS, ERP, LG_CONFIG_COLUMN50, ORGN_CDE, "
//	                        + "CREATE_BY, EDIT_BY, CREATE_DATE, EDIT_DATE, ADDRESS_DATA, LG_CONFIG_COLUMN12, LG_CONFIG_COLUMN13, LG_CONFIG_COLUMN14, LG_CONFIG_COLUMN15, LG_CONFIG_COLUMN16,"
//	                        + " LG_CONFIG_COLUMN17, LG_CONFIG_COLUMN26, UPDATE_FLAG, CREATE_FLAG, DELETE_FLAG)"
//	                        + " Values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                String insertQuery = "INSERT INTO LG_SUBSCRIPTIONS_DETAILS (REQUEST_ID, ORGN_NAME, ADDRESS_DATA, CUSTOMER_CITY, CUSTOMER_STATE,"
	                        + " CUSTOMER_COUNTRY, CUSTOMER_ZIP_CODE, CONTACT_FIRST_NAME, CONTACT_LAST_NAME, CONTACT_PHONE_NO, CONTACT_MAIL_ID,"
	                        + " LG_MODEL_TYPE, DOMAINS, ERP, ATTRIBUTE_KEY_VALUES,"
	                        + "CREATE_BY, EDIT_BY, CREATE_DATE, EDIT_DATE, INVOICE_NO, ORDER_ID, MERCHANT_ID, ORDER_CURRENCY, ORDER_AMOUNT,ADD_ONS, "
	                        + " UPDATE_FLAG, CREATE_FLAG, DELETE_FLAG, PAYMENT_STATUS, ORGN_ASSIGNED_STATUS, ORGINAL_AMOUNT, ORGINAL_DISCOUNT_AMOUNT, DISCOUNT_CODE, DISCOUNT_PCT, DISCOUNT_AMOUNT)"
	                        + " SELECT :REQUEST_ID, :ORGN_NAME, :ADDRESS_DATA, :CUSTOMER_CITY, :CUSTOMER_STATE,"
	                        + " :CUSTOMER_COUNTRY, :CUSTOMER_ZIP_CODE, :CONTACT_FIRST_NAME, :CONTACT_LAST_NAME, :CONTACT_PHONE_NO, :CONTACT_MAIL_ID,"
	                        + " :LG_MODEL_TYPE, :DOMAINS, :ERP, :ATTRIBUTE_KEY_VALUES,"
	                        + " :CREATE_BY, :EDIT_BY, "
//	                        + ":CREATE_DATE, :EDIT_DATE, "
	                        + " SYSDATE AS CREATE_DATE, SYSDATE AS EDIT_DATE, "
	                        + ":INVOICE_NO, :ORDER_ID, :MERCHANT_ID, :ORDER_CURRENCY, :ORDER_AMOUNT,:ADD_ONS, "
	                        + ":UPDATE_FLAG, :CREATE_FLAG, :DELETE_FLAG, :PAYMENT_STATUS,:ORGN_ASSIGNED_STATUS, :ORGINAL_AMOUNT, :ORGINAL_DISCOUNT_AMOUNT, :DISCOUNT_CODE, :DISCOUNT_PCT, :DISCOUNT_AMOUNT FROM  DUAL";
	                Map insertMap = new HashMap<>();
	                insertMap.put("REQUEST_ID", orgn_id);
	                insertMap.put("ORGN_NAME", textParm.get("billing_company"));
	                insertMap.put("ADDRESS_DATA", textParm.get("billing_address"));
	                insertMap.put("CUSTOMER_CITY", textParm.get("billing_city"));
	                insertMap.put("CUSTOMER_STATE", textParm.get("billing_state"));
	                insertMap.put("CUSTOMER_COUNTRY", "India");
	                insertMap.put("CUSTOMER_ZIP_CODE", textParm.get("billing_zip"));
	                insertMap.put("CONTACT_FIRST_NAME", textParm.get("billing_name"));
	                insertMap.put("CONTACT_LAST_NAME", textParm.get("billing_lastname"));
	                insertMap.put("CONTACT_PHONE_NO", textParm.get("billing_tel"));
	                insertMap.put("CONTACT_MAIL_ID", textParm.get("billing_email"));
	                insertMap.put("LG_MODEL_TYPE", textParm.get("tittle"));
	                insertMap.put("DOMAINS", "SMARTBI");
	                insertMap.put("ERP", "SMARTBI");
	                insertMap.put("ATTRIBUTE_KEY_VALUES", clobText.toString());
	                insertMap.put("CREATE_BY", "SYSTEM");
	                insertMap.put("EDIT_BY", "SYSTEM");
//	                insertMap.put("CREATE_DATE", new Date());
//	                insertMap.put("EDIT_DATE", new Date());                
	                 if(textParm.get("invoice") != null && !"".equalsIgnoreCase(textParm.get("invoice").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("invoice").toString()))
	                 {
	                 insertMap.put("INVOICE_NO", textParm.get("invoice"));   
	                 }else {
	                  insertMap.put("INVOICE_NO", "");   
	                 } 
	                 if(textParm.get("order_id") != null && !"".equalsIgnoreCase(textParm.get("order_id").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("order_id").toString()))
	                 {
	                 insertMap.put("ORDER_ID", textParm.get("order_id"));   
	                 }else {
	                 insertMap.put("ORDER_ID", "");
	                 } 
	                  if(textParm.get("merchant_id") != null && !"".equalsIgnoreCase(textParm.get("merchant_id").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("merchant_id").toString()))
	                 {
	                 insertMap.put("MERCHANT_ID", textParm.get("merchant_id"));   
	                 }else {
	                  insertMap.put("MERCHANT_ID", "");   
	                 } 
	                 if(textParm.get("amount") != null && !"".equalsIgnoreCase(textParm.get("amount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("amount").toString()))
	                 {
	                 insertMap.put("ORDER_AMOUNT", textParm.get("amount"));   
	                 }else {
	                  insertMap.put("ORDER_AMOUNT", "");   
	                 } 
	                 if(textParm.get("addON") != null && !"".equalsIgnoreCase(textParm.get("addON").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("addON").toString()))
	                 {
	                 insertMap.put("ADD_ONS", textParm.get("addON"));   
	                 }else {
	                  insertMap.put("ADD_ONS", "");   
	                 } 
	                 
	                 if(textParm.get("PAYMENT_STATUS") != null && !"".equalsIgnoreCase(textParm.get("PAYMENT_STATUS").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("PAYMENT_STATUS").toString()))
	                 {
	                 insertMap.put("PAYMENT_STATUS", textParm.get("PAYMENT_STATUS"));   
	                 }else {
	                  insertMap.put("PAYMENT_STATUS", "");   
	                 } 
	                 
	                 if(textParm.get("ORGN_ASSIGNED_STATUS") != null && !"".equalsIgnoreCase(textParm.get("ORGN_ASSIGNED_STATUS").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("ORGN_ASSIGNED_STATUS").toString()))
	                 {
	                 insertMap.put("ORGN_ASSIGNED_STATUS", textParm.get("ORGN_ASSIGNED_STATUS"));   
	                 }else {
	                  insertMap.put("ORGN_ASSIGNED_STATUS", "");   
	                 } 
	                 
	                if(textParm.get("totalOrginalAmount") != null && !"".equalsIgnoreCase(textParm.get("totalOrginalAmount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("totalOrginalAmount").toString()))
	                 {
	                 insertMap.put("ORGINAL_AMOUNT", textParm.get("totalOrginalAmount"));   
	                 }else {
	                  insertMap.put("ORGINAL_AMOUNT", "");   
	                 } 
	                if(textParm.get("totalOrginalDisAmount") != null && !"".equalsIgnoreCase(textParm.get("totalOrginalDisAmount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("totalOrginalDisAmount").toString()))
	                 {
	                 insertMap.put("ORGINAL_DISCOUNT_AMOUNT", textParm.get("totalOrginalDisAmount"));   
	                 }else {
	                  insertMap.put("ORGINAL_DISCOUNT_AMOUNT", "");   
	                 } 
	                if(textParm.get("disCouponAmmount") != null && !"".equalsIgnoreCase(textParm.get("disCouponAmmount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("disCouponAmmount").toString()))
	                 {
	                 insertMap.put("DISCOUNT_AMOUNT", textParm.get("disCouponAmmount"));   
	                 }else {
	                  insertMap.put("DISCOUNT_AMOUNT", "");   
	                 } 
	                if(textParm.get("disCouponPercentage") != null && !"".equalsIgnoreCase(textParm.get("disCouponPercentage").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("disCouponPercentage").toString()))
	                 {
	                 insertMap.put("DISCOUNT_PCT", textParm.get("disCouponPercentage"));   
	                 }else {
	                  insertMap.put("DISCOUNT_PCT", "");   
	                 } 
	                if(textParm.get("discountCode") != null && !"".equalsIgnoreCase(textParm.get("discountCode").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("discountCode").toString()))
	                 {
	                 insertMap.put("DISCOUNT_CODE", textParm.get("discountCode"));   
	                 }else {
	                  insertMap.put("DISCOUNT_CODE", "");   
	                 } 
	                
	                
	                insertMap.put("ORDER_CURRENCY", textParm.get("currency"));
	                insertMap.put("UPDATE_FLAG", "N");
	                insertMap.put("CREATE_FLAG", "N");
	                insertMap.put("DELETE_FLAG", "N");
	                System.out.println("insertMap" + insertMap);
	                System.out.println("insertQuery" + insertQuery);
	                int count = access.executeUpdateSQLNoAudit(insertQuery, insertMap);
	                System.out.println("Success" + count);
	                result = requestId;
	            } else if (flag.equalsIgnoreCase("U")) {
	            String insertQuery = "UPDATE LG_SUBSCRIPTIONS_DETAILS SET PAYMENT_STATUS = :PAYMENT_STATUS,ORGN_ASSIGNED_STATUS =:ORGN_ASSIGNED_STATUS "
	                        + " WHERE REQUEST_ID = :REQUEST_ID";  
	             Map insertMap = new HashMap<>();
	                insertMap.put("REQUEST_ID", requestId);
	                 if(textParm.get("PAYMENT_STATUS") != null && !"".equalsIgnoreCase(textParm.get("PAYMENT_STATUS").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("PAYMENT_STATUS").toString()))
	                 {
	                 insertMap.put("PAYMENT_STATUS", textParm.get("PAYMENT_STATUS"));   
	                 }else {
	                  insertMap.put("PAYMENT_STATUS", "");   
	                 } 
	                 
	                 if(textParm.get("ORGN_ASSIGNED_STATUS") != null && !"".equalsIgnoreCase(textParm.get("ORGN_ASSIGNED_STATUS").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("ORGN_ASSIGNED_STATUS").toString()))
	                 {
	                 insertMap.put("ORGN_ASSIGNED_STATUS", textParm.get("ORGN_ASSIGNED_STATUS"));   
	                 }else {
	                  insertMap.put("ORGN_ASSIGNED_STATUS", "");   
	                 } 
	                
	                  int count = access.executeUpdateSQLNoAudit(insertQuery, insertMap);
	                System.out.println("Success" + count);
	                result = requestId;
	                
	            }else {
//	                String status = (String) request.getParameter("status");
//	                byte[] decodedBytes = Base64.getDecoder().decode(status);
//	                String decodedString = new String(decodedBytes);
	                String updateFlag = (String) textParm.get("Update");
	                StringBuilder clobText = new StringBuilder();
	                for (Object key : textParm.keySet()) {
	                    Object value = textParm.get(key);
	                    clobText.append("Key: " + key + ", Value: " + value + ",");
	                }
//	                String insertQuery = "UPDATE DAL_LG_CONFIG SET UPDATE_FLAG=:UPDATE_FLAG WHERE ORGN_ID=:ORGN_ID";
	                String insertQuery = "UPDATE LG_SUBSCRIPTIONS_DETAILS SET (ORGN_NAME, ADDRESS_DATA, CUSTOMER_CITY, CUSTOMER_STATE,"
	                        + " CUSTOMER_COUNTRY, CUSTOMER_ZIP_CODE, CONTACT_FIRST_NAME, CONTACT_LAST_NAME, CONTACT_PHONE_NO, CONTACT_MAIL_ID,"
	                        + " LG_MODEL_TYPE, DOMAINS, ERP, ATTRIBUTE_KEY_VALUES,"
	                        + " EDIT_BY, EDIT_DATE, INVOICE_NO, ORDER_ID, MERCHANT_ID, ORDER_CURRENCY, ORDER_AMOUNT,ADD_ONS, "
	                        + " UPDATE_FLAG, CREATE_FLAG, DELETE_FLAG, PAYMENT_STATUS,ORGN_ASSIGNED_STATUS, ORGINAL_AMOUNT, ORGINAL_DISCOUNT_AMOUNT, DISCOUNT_CODE, DISCOUNT_PCT, DISCOUNT_AMOUNT )"
	                        + " = (SELECT :ORGN_NAME, :ADDRESS_DATA, :CUSTOMER_CITY, :CUSTOMER_STATE,"
	                        + ":CUSTOMER_COUNTRY, :CUSTOMER_ZIP_CODE, :CONTACT_FIRST_NAME, :CONTACT_LAST_NAME, :CONTACT_PHONE_NO, :CONTACT_MAIL_ID,"
	                        + ":LG_MODEL_TYPE, :DOMAINS, :ERP, :ATTRIBUTE_KEY_VALUES,"
	                        + ":EDIT_BY, :EDIT_DATE, :INVOICE_NO, :ORDER_ID, :MERCHANT_ID, :ORDER_CURRENCY, :ORDER_AMOUNT,:ADD_ONS, "
	                        + ":UPDATE_FLAG, :CREATE_FLAG, :DELETE_FLAG, :PAYMENT_STATUS,:ORGN_ASSIGNED_STATUS , :ORGINAL_AMOUNT, :ORGINAL_DISCOUNT_AMOUNT, :DISCOUNT_CODE, :DISCOUNT_PCT, :DISCOUNT_AMOUNT FROM DUAL) WHERE REQUEST_ID = :REQUEST_ID";
	                Map insertMap = new HashMap<>();
	                insertMap.put("REQUEST_ID", requestId);
	                insertMap.put("ORGN_NAME", textParm.get("billing_company"));
	                insertMap.put("ADDRESS_DATA", textParm.get("billing_address"));
	                insertMap.put("CUSTOMER_CITY", textParm.get("billing_city"));
	                insertMap.put("CUSTOMER_STATE", textParm.get("billing_state"));
	                insertMap.put("CUSTOMER_COUNTRY", textParm.get("billing_country"));
	                insertMap.put("CUSTOMER_ZIP_CODE", textParm.get("billing_zip"));
	                insertMap.put("CONTACT_FIRST_NAME", textParm.get("billing_name"));
	                insertMap.put("CONTACT_LAST_NAME", textParm.get("billing_lastname"));
	                insertMap.put("CONTACT_PHONE_NO", textParm.get("billing_tel"));
	                insertMap.put("CONTACT_MAIL_ID", textParm.get("billing_email"));
	                insertMap.put("LG_MODEL_TYPE", textParm.get("tittle"));
	                insertMap.put("DOMAINS", "SMARTBI");
	                insertMap.put("ERP", "SMARTBI");
	                insertMap.put("ATTRIBUTE_KEY_VALUES", clobText.toString());
//	                insertMap.put("CREATE_BY", "SYSTEM");
	                insertMap.put("EDIT_BY", "SYSTEM");
//	                insertMap.put("CREATE_DATE", new Date());
	                insertMap.put("EDIT_DATE", new Date());                
	                 if(textParm.get("invoice") != null && !"".equalsIgnoreCase(textParm.get("invoice").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("invoice").toString()))
	                 {
	                 insertMap.put("INVOICE_NO", textParm.get("invoice"));   
	                 }else {
	                  insertMap.put("INVOICE_NO", "");   
	                 } 
	                 if(textParm.get("order_id") != null && !"".equalsIgnoreCase(textParm.get("order_id").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("order_id").toString()))
	                 {
	                 insertMap.put("ORDER_ID", textParm.get("order_id"));   
	                 }else {
	                 insertMap.put("ORDER_ID", "");
	                 }  
	                  if(textParm.get("merchant_id") != null && !"".equalsIgnoreCase(textParm.get("merchant_id").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("merchant_id").toString()))
	                 {
	                 insertMap.put("MERCHANT_ID", textParm.get("merchant_id"));   
	                 }else {
	                  insertMap.put("MERCHANT_ID", "");   
	                 } 
	                 if(textParm.get("amount") != null && !"".equalsIgnoreCase(textParm.get("amount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("amount").toString()))
	                 {
	                 insertMap.put("ORDER_AMOUNT", textParm.get("amount"));   
	                 }else {
	                  insertMap.put("ORDER_AMOUNT", "");   
	                 } 
	                 if(textParm.get("addON") != null && !"".equalsIgnoreCase(textParm.get("addON").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("addON").toString()))
	                 {
	                 insertMap.put("ADD_ONS", textParm.get("addON"));   
	                 }else {
	                  insertMap.put("ADD_ONS", "");   
	                 } 
	                 
	                if(textParm.get("PAYMENT_STATUS") != null && !"".equalsIgnoreCase(textParm.get("PAYMENT_STATUS").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("PAYMENT_STATUS").toString()))
	                 {
	                 insertMap.put("PAYMENT_STATUS", textParm.get("PAYMENT_STATUS"));   
	                 }else {
	                  insertMap.put("PAYMENT_STATUS", "");   
	                 } 
	                 
	                 if(textParm.get("ORGN_ASSIGNED_STATUS") != null && !"".equalsIgnoreCase(textParm.get("ORGN_ASSIGNED_STATUS").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("ORGN_ASSIGNED_STATUS").toString()))
	                 {
	                 insertMap.put("ORGN_ASSIGNED_STATUS", textParm.get("ORGN_ASSIGNED_STATUS"));   
	                 }else {
	                  insertMap.put("ORGN_ASSIGNED_STATUS", "");   
	                 } 
	                 
	                  if(textParm.get("totalOrginalAmount") != null && !"".equalsIgnoreCase(textParm.get("totalOrginalAmount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("totalOrginalAmount").toString()))
	                 {
	                 insertMap.put("ORGINAL_AMOUNT", textParm.get("totalOrginalAmount"));   
	                 }else {
	                  insertMap.put("ORGINAL_AMOUNT", "");   
	                 } 
	                if(textParm.get("totalOrginalDisAmount") != null && !"".equalsIgnoreCase(textParm.get("totalOrginalDisAmount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("totalOrginalDisAmount").toString()))
	                 {
	                 insertMap.put("ORGINAL_DISCOUNT_AMOUNT", textParm.get("totalOrginalDisAmount"));   
	                 }else {
	                  insertMap.put("ORGINAL_DISCOUNT_AMOUNT", "");   
	                 } 
	                if(textParm.get("disCouponAmmount") != null && !"".equalsIgnoreCase(textParm.get("disCouponAmmount").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("disCouponAmmount").toString()))
	                 {
	                 insertMap.put("DISCOUNT_AMOUNT", textParm.get("disCouponAmmount"));   
	                 }else {
	                  insertMap.put("DISCOUNT_AMOUNT", "");   
	                 } 
	                if(textParm.get("disCouponPercentage") != null && !"".equalsIgnoreCase(textParm.get("disCouponPercentage").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("disCouponPercentage").toString()))
	                 {
	                 insertMap.put("DISCOUNT_PCT", textParm.get("disCouponPercentage"));   
	                 }else {
	                  insertMap.put("DISCOUNT_PCT", "");   
	                 } 
	                if(textParm.get("discountCode") != null && !"".equalsIgnoreCase(textParm.get("discountCode").toString())
	                         && !"null".equalsIgnoreCase(textParm.get("discountCode").toString()))
	                 {
	                 insertMap.put("DISCOUNT_CODE", textParm.get("discountCode"));   
	                 }else {
	                  insertMap.put("DISCOUNT_CODE", "");   
	                 } 
	                 
	                insertMap.put("ORDER_CURRENCY", textParm.get("currency"));
	                insertMap.put("UPDATE_FLAG", "N");
	                insertMap.put("CREATE_FLAG", "N");
	                insertMap.put("DELETE_FLAG", "N");
	                int count = access.executeUpdateSQLNoAudit(insertQuery, insertMap);
	                System.out.println("Success" + count);
//	                
//	                Map insertMap = new HashMap<>();
//	                insertMap.put("UPDATE_FLAG", "Y");
//	                insertMap.put("ORGN_ID", decodedString);
//	                int count = access.executeUpdateSQLNoAudit(insertQuery, insertMap);
//	                result = "Success";
	                result = requestId;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	    
	    public String getverificationcode(HttpServletRequest req) {
	        boolean test = false;
	        int length = 8;
	        boolean useLetters = false;
	        boolean useNumbers = true;
	        String otp = "";
	        String otpnumber = "";
	        String fromEmail = "pilogvision1@piloggroup.org";
	        String password = "waxvwnxwgaoaaikf";
	        try {
	            String toEmail = req.getParameter("emailId");
	            Properties pr = new Properties();
	            pr.setProperty("mail.smtp.host", "smtp.gmail.com");
	            pr.setProperty("mail.smtp.port", "587");
	            pr.setProperty("mail.smtp.auth", "true");
	            pr.setProperty("mail.smtp.starttls.enable", "true");
	            pr.put("mail.smtp.socketFactory.port", "587");
	            pr.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	            Session session = Session.getInstance(pr,
	                    new javax.mail.Authenticator() {
	                @Override
	                protected PasswordAuthentication getPasswordAuthentication() {
	                    return new PasswordAuthentication(fromEmail, password);
	                }
	            });
	            String generatedString = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
	            String encodedString = Base64.getEncoder().encodeToString(generatedString.getBytes());
	            req.getSession(false).setAttribute("PaymentVerifyOTP", encodedString);
	            String mailText = "Please enter the below mentioned OTP: " + generatedString;
	            Message mess = new MimeMessage(session);
	            mess.setFrom(new InternetAddress(fromEmail));
	            mess.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
	            mess.setSubject("User Email Verification");
	            mess.setText(mailText);
	            Transport.send(mess);
	            test = true;
	            if (test) {
	                otpnumber = "Success";
	            } else {
	                otpnumber = "";
	            }

	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        return otpnumber;
	    }
	    
	    @Transactional(propagation = Propagation.REQUIRES_NEW)
	    public JSONObject getApplyDiscountCode(HttpServletRequest request) {
	        JSONObject resultObj = new JSONObject();
	        try {
	            String discountCode = (String) request.getParameter("discountCode");
	            String query = "SELECT DISCOUNT_PCT FROM LG_DISCOUNT_CODES WHERE DISCOUNT_CODE = :DISCOUNT_CODE"
	                    + " AND TO_DATE(TO_CHAR(EXPIRY_DATE,'DD-MM-YYYY'),'DD-MM-YYYY') >= TO_DATE(TO_CHAR(SYSDATE,'DD-MM-YYYY'),'DD-MM-YYYY')"
	                    + " AND USAGE_COUNT < USAGE_LIMIT ";
	            Map queryMap = new HashMap();
	            queryMap.put("DISCOUNT_CODE", discountCode);
	            List resultList = access.sqlqueryWithParams(query, queryMap);
	            if (resultList != null && !resultList.isEmpty()) {
	                String discountPct = (String) resultList.get(0);
	                resultObj.put("discountPct", discountPct);
	                resultObj.put("status", true);
	                resultObj.put("msg", "correct");
	                String updateQuery = "UPDATE LG_DISCOUNT_CODES SET USAGE_COUNT = USAGE_COUNT+1 WHERE DISCOUNT_CODE = :DISCOUNT_CODE";
	                int count = access.executeUpdateSQLNoAudit(updateQuery, queryMap);
	                System.out.println("Success" + count);
	            } else {
	                resultObj.put("status", false);
	                resultObj.put("msg", "Invalid Discount Code");
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	    
	    public String sendEmailText(HttpServletRequest request, String text, String subject, String email, String column, JSONObject textParm) {
	        String result = "";
	        try {
//	            String fileNamePath2 = "C:\\MailFiles" + File.separator + "mail"; // Modify the file name and extension
//	            File folder3 = new File(fileNamePath2);
//	            File path2 = new File(folder3.getAbsolutePath() + File.separator + "Invoice.pdf");
//	            path2.delete();
	            //deleate fille path
	            String loc1 = "mail";
	            String dirName1 = fileSubStoreHomedirectory+"MailFiles" + File.separator + loc1;
	            File folder1 = new File(dirName1);
	            folder1.delete();
	            //deleate fille folder path  
	            boolean test = false;
	            JSONObject mailConfig = getDalMailConfigData();
	            String fromEmail = mailConfig.get("USER_NAME").toString();
	            String password = mailConfig.get("PASWORD").toString();
	            Properties pr = new Properties();
	            pr.setProperty("mail.smtp.host", mailConfig.get("SMTP_HOST").toString());
	            pr.setProperty("mail.smtp.port", mailConfig.get("SMTP_PORT").toString());
	            pr.setProperty("mail.smtp.auth", mailConfig.get("SMTP_AUTH").toString());
	            pr.setProperty("mail.smtp.starttls.enable", mailConfig.get("SMTP_STARTTLS_ENABLE").toString());
	            pr.put("mail.smtp.socketFactory.port", "587");
	            pr.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	            Session session = Session.getInstance(pr,
	                    new javax.mail.Authenticator() {
	                @Override
	                protected PasswordAuthentication getPasswordAuthentication() {
	                    return new PasswordAuthentication(fromEmail, password);
	                }
	            });
	            String loc = "mail";
	            String dirName = fileSubStoreHomedirectory+"MailFiles" + File.separator + loc;
	            File folder = new File(dirName);
	            if (!folder.exists()) {
	                folder.mkdirs();
	                File mailFile = new File(folder.getAbsolutePath() + File.separator + "Invoice.pdf");
	                OutputStream out = new FileOutputStream(mailFile);
	                out.close();
	            }
	            //html to pdf
	            String fileNamePath1 = fileSubStoreHomedirectory+"MailFiles" + File.separator + "mail"; // Modify the file name and extension
	            File folder2 = new File(fileNamePath1);
	            File path1 = new File(folder2.getAbsolutePath() + File.separator + "Invoice.pdf");
	            OutputStream file = new FileOutputStream(new File(path1.toString()));
//	            Document document = new Document();
//	            PdfWriter.getInstance(document, file);
//	            document.open();
//	            HTMLWorker htmlWorker = new HTMLWorker(document);
//	            htmlWorker.parse(new StringReader(text));
//	            document.close();
//	            file.close();
	            //html to pdf
	            //xml formate convert pdf
	            Document document = new Document();
	            PdfWriter writer = PdfWriter.getInstance(document, file);
	            document.open();
	            InputStream is = new ByteArrayInputStream(text.getBytes());
	            XMLWorkerHelper.getInstance().parseXHtml(writer, document, is);
	            document.close();
	            file.close();
	//xml formate convert pdf
	            //html to pdf
	            //receive text mail
//	            String testParam = (String) request.getSession(false).getAttribute("testParam");
//	            byte[] decodedBytes1 = Base64.getUrlDecoder().decode(testParam);
//	            String objStrData1 = new String(decodedBytes1);
//	            JSONObject textParm = (JSONObject) JSONValue.parse(objStrData1);
	            String mailText = "<div><span>Dear " + textParm.get("billing_name") + ",<br>"
	                    + "Congratulations your subscription process is completed. <br>"
	                    + "We have received payment of Rs. " + textParm.get("amount") + ". Please find the attached Payment"
	                    + " Receipt for smartBi IG subscription, Your Order No: " + textParm.get("order_id") + ". <br>"
	                    + "Thank you and feel free to contact us for support on <a href='mailto:support.application@piloggroup.com'>support.application@piloggroup.com</a>.</br>"
	                    + "<span style='color:blue'> Please use this Username to login :"+textParm.get("userName")+"</span>.</br>"
	                    + "The Url is: <a href='https://smart.integraldataanalytics.com' style='color:blue'>https://smart.integraldataanalytics.com</a></span></div>";


	            //receive text mail
	            //send to mail
	            MimeMessage message = new MimeMessage(session);
	            message.setFrom(new InternetAddress(fromEmail));
	            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
	            message.setSubject("Payment Recipt");

	            // Create the HTML body part
	            MimeBodyPart htmlBodyPart = new MimeBodyPart();
	            htmlBodyPart.setContent(mailText, "text/html");

	            // Create the PDF attachment part
	            MimeBodyPart pdfBodyPart = new MimeBodyPart();

	            String fileNamePath = fileSubStoreHomedirectory+"MailFiles" + File.separator + "mail"; // Modify the file name and extension
	            File path = new File(fileNamePath);
	            if (path.exists()) {
	                String files[] = path.list();
	                if (files != null && files.length > 0) {
	                    for (String tempFile : files) {
	                        String totalFilePath = path + File.separator + tempFile;
	                        DataSource source = new FileDataSource(totalFilePath);
	                        pdfBodyPart.setDataHandler(new DataHandler(source));
	                        pdfBodyPart.setFileName(source.getName());
	                    }
	                }
	            }

	            // Create a multipart/alternative part to combine the HTML and PDF parts
	            MimeMultipart multipartAlternative = new MimeMultipart("alternative");
	            multipartAlternative.addBodyPart(htmlBodyPart);

	            // Create a multipart/mixed part to hold the multipart/alternative and PDF parts
	            MimeMultipart multipartMixed = new MimeMultipart("mixed");
	            MimeBodyPart multipartAlternativePart = new MimeBodyPart();
	            multipartAlternativePart.setContent(multipartAlternative);

	            // Add the multipart/alternative and PDF parts to the multipart/mixed part
	            multipartMixed.addBodyPart(multipartAlternativePart);
	            multipartMixed.addBodyPart(pdfBodyPart);

	            // Set the content of the message as the multipart/mixed
	            message.setContent(multipartMixed);

	            // Send the email
	            Transport.send(message);
	            
	            test = true;
	            if (test) {
	                result = "Success";
	            } else {
	                result = "";
	            }
	            //send to mail
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	    
	    private JSONObject getDalMailConfigData() {
	        JSONObject resultObj = new JSONObject();
	        try {
	            String query = "SELECT SMTP_HOST, TRANSPORT_PROTOCOL, SMTP_STARTTLS_ENABLE, SMTP_AUTH, SMTP_PORT, USER_NAME, PASWORD FROM DAL_MAIL_CONFIG WHERE ORGN_ID=:ORGN_ID";
	            Map queryMap = new HashMap();
	            queryMap.put("ORGN_ID", "C1F5CFB03F2E444DAE78ECCEAD80D27D");
	            List resultList = access.sqlqueryWithParams(query, queryMap);
	            if (resultList != null && !resultList.isEmpty()) {
	                for (Object dataObj : resultList) {
	                    Object[] dataArrObj = (Object[]) dataObj;
	                    resultObj.put("SMTP_HOST", dataArrObj[0]);
	                    resultObj.put("TRANSPORT_PROTOCAL", dataArrObj[1]);
	                    resultObj.put("SMTP_STARTTLS_ENABLE", dataArrObj[2]);
	                    resultObj.put("SMTP_AUTH", dataArrObj[3]);
	                    resultObj.put("SMTP_PORT", dataArrObj[4]);
	                    resultObj.put("USER_NAME", dataArrObj[5]);
	                    resultObj.put("PASWORD", dataArrObj[6]);
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	    
	    public JSONObject getDBAllTranstionDetails(HttpServletRequest request) {
	        JSONObject resultObject = new JSONObject();
	        try {
	            String status = (String) request.getParameter("status");
	            byte[] decodedBytes = Base64.getDecoder().decode(status);
	            String decodedString = new String(decodedBytes);
//	            String query = "SELECT ORGN_NAME, CUSTOMER_CITY, CUSTOMER_STATE,"
//	                    + " CUSTOMER_COUNTRY, CONTACT_FIRST_NAME, CONTACT_LAST_NAME, CONTACT_PHONE_NO, CONTACT_MAIL_ID,"
//	                    + " LG_MODEL_TYPE, DEFAULT_LG_MODEL_ORGN_ID, DOMAINS, ERP,"
//	                    + " ADDRESS_DATA, INVOICE_NO, LG_CONFIG_COLUMN13, ORDER_ID, MERCHANT_ID,"
//	                    + " ORDER_AMOUNT, CUSTOMER_ZIP_CODE"
//	                    + " FROM DAL_LG_CONFIG WHERE ORGN_ID=:ORGN_ID";
//	            
	            String query = " SELECT ORGN_NAME, ADDRESS_DATA, CUSTOMER_CITY, CUSTOMER_STATE,"
	                        + " CUSTOMER_COUNTRY, CUSTOMER_ZIP_CODE, CONTACT_FIRST_NAME, CONTACT_LAST_NAME, "
	                        + " CONTACT_PHONE_NO, CONTACT_MAIL_ID,"
	                        + " LG_MODEL_TYPE, DOMAINS, ERP, ATTRIBUTE_KEY_VALUES,"
	                        + " CREATE_BY, EDIT_BY, "
	                        + " CREATE_DATE, EDIT_DATE, "
	                        + "INVOICE_NO, ORDER_ID, MERCHANT_ID, ORDER_CURRENCY, ORDER_AMOUNT, ADD_ONS, "
	                        + " PAYMENT_STATUS,ORGN_ASSIGNED_STATUS FROM  LG_SUBSCRIPTIONS_DETAILS"
	                        + " WHERE REQUEST_ID = :REQUEST_ID ";
	            Map queryMap = new HashMap<>();
	            queryMap.put("REQUEST_ID", decodedString);
	            List resultList = access.sqlqueryWithParams(query, queryMap);
	            if (resultList != null && !resultList.isEmpty()) {
	                for (Object objdata : resultList) {
	                    Object[] objArrData = (Object[]) objdata;
	                    resultObject.put("billing_company", objArrData[0]);
	                    resultObject.put("ORGN_NAME", objArrData[0]);
	                    resultObject.put("billing_address", objArrData[1]);
	                    resultObject.put("billing_city", objArrData[2]);
	                    resultObject.put("billing_state", objArrData[3]);
	                    resultObject.put("billing_country", objArrData[4]);
	                    resultObject.put("billing_zip", objArrData[5]);
	                    resultObject.put("billing_name", objArrData[6]);
	                    resultObject.put("billing_lastname", objArrData[7]);
	                    resultObject.put("billing_tel", objArrData[8]);
	                    resultObject.put("billing_email", objArrData[9]);
	                    resultObject.put("tittle", objArrData[10]);
	                    resultObject.put("Domain", objArrData[11]);
	                    resultObject.put("ERP", objArrData[12]);
//	                    resultObject.put("ATTRIBUTE_KEY_VALUES", cloudUtills.clobToString((Clob)objArrData[13]));
	                    resultObject.put("invoice", objArrData[18]);
	                    resultObject.put("order_id", objArrData[19]);
	                    resultObject.put("merchant_id", objArrData[20]);
	                     resultObject.put("currency", objArrData[21]);
	                    resultObject.put("amount", objArrData[22]);
//	                    resultObject.put("addON", new PiLogCloudUtills().clobToString((Clob)objArrData[23])); 
	                    resultObject.put("addON", objArrData[23]); 
	                    resultObject.put("requestId", decodedString);
	                   
	                }
	            }
	            
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObject;
	    }
	    
	    @Transactional
		public JSONObject fetchGanttChartData(HttpServletRequest request) {
			JSONObject chartObj = new JSONObject();
			try {
				String whereCondQuery = "";
				String chartId = request.getParameter("chartId");
				String axisColumns = request.getParameter("axisColumns");
				String valuesColumns = request.getParameter("valuesColumns");
				String comboColumns = request.getParameter("comboColumns");
				String percentColumns = request.getParameter("zAxixValues");
				String filterColumns = request.getParameter("filterColumns");
				String tables = request.getParameter("tablesObj");
				String chartType = request.getParameter("chartType");
				String JoinQuery = request.getParameter("joinQuery");
				String selectedvalue = request.getParameter("selectedValue");
				String Slicecolumn = request.getParameter("SliceColumn");
				String dragtableName = request.getParameter("dragtableName");
				JSONArray axisColsArr = new JSONArray();
				JSONArray valuesColsArr = new JSONArray();
				JSONArray comboColsArr = new JSONArray();
				JSONArray percentColsArr = new JSONArray();
				JSONArray filterColsArr = new JSONArray();
				JSONArray colsArr = new JSONArray();
				JSONArray tablesArr = new JSONArray();
				JSONObject filteredChartConfigObj = new JSONObject();
				JSONObject chartConfigObj = new JSONObject();
				String chartConfigObjStr = request.getParameter("chartCOnfigObjStr");
				String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
				if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
						&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
					chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
				}
				for (Object chartKey : chartConfigObj.keySet()) {
					String key = String.valueOf(chartKey);
					String filteredKey = key.replaceAll("\\d", "");
					filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
				}
				JSONObject configObj = buildOptionsObj(request, filteredChartConfigObj, chartConfigPositionKeyStr, chartId,
						chartType);
				JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
				JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
				if (axisColumns != null && !"".equalsIgnoreCase(axisColumns) && !"null".equalsIgnoreCase(axisColumns)) {
					axisColsArr = (JSONArray) JSONValue.parse(axisColumns);
				}
				if (valuesColumns != null && !"".equalsIgnoreCase(valuesColumns)
						&& !"null".equalsIgnoreCase(valuesColumns)) {
					valuesColsArr = (JSONArray) JSONValue.parse(valuesColumns);
				}
				if (comboColumns != null && !"".equalsIgnoreCase(comboColumns) && !"null".equalsIgnoreCase(comboColumns)) {
					comboColsArr = (JSONArray) JSONValue.parse(comboColumns);
				}
				if (percentColumns != null && !"".equalsIgnoreCase(percentColumns) && !"null".equalsIgnoreCase(percentColumns)) {
					percentColsArr = (JSONArray) JSONValue.parse(percentColumns);
				}
				
				if (filterColumns != null && !"".equalsIgnoreCase(filterColumns)
						&& !"null".equalsIgnoreCase(filterColumns)) {
					filterColsArr = (JSONArray) JSONValue.parse(filterColumns);
				}

				if (tables != null && !"".equalsIgnoreCase(tables) && !"null".equalsIgnoreCase(tables)) {
					tablesArr = (JSONArray) JSONValue.parse(tables);
				}
				
				if (axisColsArr != null && !axisColsArr.isEmpty()) {
					for (int i = 0; i < axisColsArr.size(); i++) {
						JSONObject axisColObj = (JSONObject) axisColsArr.get(i);
						if (axisColObj != null && !axisColObj.isEmpty()) {
							String columnName = (String) axisColObj.get("columnName");
							if (columnName != null && !"".equalsIgnoreCase(columnName)) {
								colsArr.add(columnName);
							}
						}
					}
				}

				if (valuesColsArr != null && !valuesColsArr.isEmpty()) {
					for (int i = 0; i < valuesColsArr.size(); i++) {
						JSONObject valueColObj = (JSONObject) valuesColsArr.get(i);
						if (valueColObj != null && !valueColObj.isEmpty()) {
							String columnName = (String) valueColObj.get("columnName");
							if (columnName != null && !"".equalsIgnoreCase(columnName)) {
								colsArr.add(columnName);
							}
						}
					}
				}
				
				if (comboColsArr != null && !comboColsArr.isEmpty()) {
					for (int i = 0; i < comboColsArr.size(); i++) {
						JSONObject comboColObj = (JSONObject) comboColsArr.get(i);
						if (comboColObj != null && !comboColObj.isEmpty()) {
							String columnName = (String) comboColObj.get("columnName");
							if (columnName != null && !"".equalsIgnoreCase(columnName)) {
								colsArr.add(columnName);
							}
						}
					}
				}
				
				if (percentColsArr != null && !percentColsArr.isEmpty()) {
					for (int i = 0; i < percentColsArr.size(); i++) {
						JSONObject percentColObj = (JSONObject) percentColsArr.get(i);
						if (percentColObj != null && !percentColObj.isEmpty()) {
							String columnName = (String) percentColObj.get("columnName");
							if (columnName != null && !"".equalsIgnoreCase(columnName)) {
								colsArr.add(columnName);
							}
						}
					}
				}

				if (filterColsArr != null && !filterColsArr.isEmpty()) {
					for (int i = 0; i < filterColsArr.size(); i++) {
						JSONObject filterColObj = (JSONObject) filterColsArr.get(i);
						if (filterColObj != null && !filterColObj.isEmpty()) {
							whereCondQuery += buildCondition(filterColObj, request);
							if (i != filterColsArr.size() - 1) {
								whereCondQuery += " AND ";
							}
						}
					}
				}
				if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery) && Slicecolumn != null
						&& !"".equalsIgnoreCase(Slicecolumn)) {
					whereCondQuery += dragtableName + "." + Slicecolumn + " ";
					whereCondQuery += "IN";
					String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
					whereCondQuery += value;
				} else {
					if (selectedvalue != null && !"".equalsIgnoreCase(selectedvalue) && Slicecolumn != null
							&& !"".equalsIgnoreCase(Slicecolumn)) {
						String value = "('" + selectedvalue.replaceAll(",", "','") + "')";
						whereCondQuery += Slicecolumn + " ";
						whereCondQuery += "IN";
						whereCondQuery += value;
					}
				}

				if (whereCondQuery != null && !"".equalsIgnoreCase(whereCondQuery)
						&& !"null".equalsIgnoreCase(whereCondQuery)) {
					whereCondQuery = " WHERE " + whereCondQuery;
				}
				JSONArray resultArr = new JSONArray();
				if (tablesArr != null && !tablesArr.isEmpty()) {
					String tableName = (String) tablesArr.get(0);
					if (JoinQuery != null && !"".equalsIgnoreCase(JoinQuery)) {
						tableName = JoinQuery;
					}else {
						tableName = " FROM "+tableName;
					}
					if(colsArr !=null && !colsArr.isEmpty())
					{
						JSONArray jsLabelArr = new JSONArray();
						String[] labelsArr = {"Task ID","Task Name","Start Date","End Date","Duration","Percent Complete","Dependencies"};
						String[] labelTypesArr = {"string","string","date","date","number","number","string"};
						for (int l = 0; l < labelsArr.length; l++) {
                            String labelData = labelsArr[l];
                            String labelTypeData = labelTypesArr[l];
                            JSONObject jsLabelsData = new JSONObject();
                            if (labelData != null && !"".equalsIgnoreCase(labelData)) {
                            	jsLabelsData.put("label", labelData);
                                jsLabelsData.put("id", labelData);
                                jsLabelsData.put("type", labelTypeData);
                                jsLabelArr.add(jsLabelsData);
                            }
                        }
						resultArr.add(jsLabelArr);
						String orderBy = " ORDER BY "+(String)colsArr.get(0);
					    String query =(String) colsArr.stream().filter(e -> (e !=null)).map(f->f).collect(Collectors.joining(","));
					    query = "SELECT "+query +" "+ tableName + whereCondQuery + orderBy;
					    List selectData = access.sqlqueryWithParamsLimit(query, new HashMap(), 10, 0); 
					    if(selectData !=null && !selectData.isEmpty())
					    {
					    	String obj = null;
					    	for(int i=0;i<selectData.size();i++)
					    	{
					    		JSONArray dataArr = new JSONArray();
					    		Object[] objData = (Object[])selectData.get(i);
					    		if(objData !=null)
					    		{
					    			Date startDate = (Date)objData[1];
					    			Date endDate = (Date)objData[2];
					    			int startYear = startDate.getYear();int startMonth = startDate.getMonth(); int startDay = startDate.getDate();
					    			int endYear = endDate.getYear();int endMonth = endDate.getMonth(); int endDay = endDate.getDate();
					    			dataArr.add(objData[0]);
					    			dataArr.add(objData[0]);
					    			dataArr.add((startYear+1900)+","+(startMonth+1)+","+startDay);
					    			dataArr.add((endYear+1900)+","+(endMonth+1)+","+endDay);
					    			dataArr.add(obj);
					    			dataArr.add(objData[3]);
					    			dataArr.add(obj);
					    		}
					    		resultArr.add(dataArr);
					    	}
					    }
					    
					}


					
				}
				
				chartObj.put("data", resultArr);
				chartObj.put("chartCOnfigObjStr",chartConfigObjStr);
			} catch (Exception ex) { 
				ex.printStackTrace(); 
			}
			return chartObj;  
		}
	    
	    
	    @Transactional
	    public String createOrgn(HttpServletRequest request,JSONObject textParam)
	    {
	    	String orgnId = "";
	    	try {
	    	String orgnName = (String)textParam.get("ORGN_NAME");
	    	if(orgnName !=null && !"".equalsIgnoreCase(orgnName))
	    	{
	    		String insertQuery  = "INSERT INTO ORGN_STRUCTURE(ORGN_ID,NAME,CREATE_DATE,CREATE_BY,EDIT_DATE,EDIT_BY,"
	    				+ "ORGN_TYPE,ACTIVE_FLAG) VALUES(?,?,?,?,?,?,?,?)";
	    		Map mapData = new HashMap();
	    		String orgId = AuditIdGenerator.genRandom32Hex();
	    		mapData.put(1, orgId);
	    		mapData.put(2, orgnName);
	    		mapData.put(3, new Date());
	    		mapData.put(4, "Integral");
	    		mapData.put(5, new Date());
	    		mapData.put(6, "Integral");
	    		mapData.put(7, "V");
	    		mapData.put(8, "Y");
	    		int count = access.executeNativeUpdateSQLWithSimpleParamsNoAudit(insertQuery, mapData);
	    		if(count>0)
	    		{
	    			orgnId = orgId;
	    		}
	    	}
	    		
	    	}catch(Exception ex)
	    	{
	    		ex.printStackTrace();
	    	}
	    	return orgnId;
	    }
	    
	    @Transactional
	    public int updatePasswordParamFlagForNewSubscriptedUsers(HttpServletRequest request, JSONObject basicData)
	    {
	      int count = 0;
	      try {
	    	  String userNameReq = "";
	    	  String persid = "";
	            if(basicData !=  null && !basicData.isEmpty()){
	            	userNameReq = (String) basicData.get("billing_name")+"_"+(String) basicData.get("billing_lastname")+"_MGR";
	                userNameReq = userNameReq.toUpperCase();
	            }
	           persid = cloudSheduleDAO.getPersId(userNameReq, request);
	           String updateQuery = "UPDATE S_PERSONNEL SET PASSWORD_FLAG=:PASSWORD_FLAG WHERE PERS_ID=:PERS_ID";
	           Map updateMap = new HashMap();
	           updateMap.put("PASSWORD_FLAG", "N");
	           updateMap.put("PERS_ID", persid);
	           count = access.executeUpdateSQL(updateQuery, updateMap);
	      }catch(Exception ex)
	      {
	    	ex.printStackTrace();  
	      }
	      return count;
	    }
	    
	    
	    @Transactional
	    public int updateNoofUsersForNewSubscription(HttpServletRequest request, JSONObject basicData)
	    {
	      int count = 0;
	      try {
	    	  String role_Id = "ADMIN";
	    	  String orgnId = "";
	    	  String igType = "";
	            if(basicData !=  null && !basicData.isEmpty()){
	            	orgnId =(String)basicData.get("orgnId");
	            	igType = (String)basicData.get("tittle");
	            }
	           String updateQuery = "UPDATE C_ROLE_USERS SET NO_OF_USERS=:NO_OF_USERS WHERE ROLE_ID=:ROLE_ID AND ORGN_ID=:ORGN_ID";
	           Map updateMap = new HashMap();
	           updateMap.put("ROLE_ID", "MM_MANAGER");
	           updateMap.put("ORGN_ID", orgnId);
	           if(igType !=null && !"".equalsIgnoreCase(igType) && "Basic".equalsIgnoreCase(igType))
	           {
	        	   updateMap.put("NO_OF_USERS", 1); 
	           }
	           else if(igType !=null && !"".equalsIgnoreCase(igType) && "Professional".equalsIgnoreCase(igType))
	           {
	        	   updateMap.put("NO_OF_USERS", 5);
	           }
	           count = access.executeUpdateSQL(updateQuery, updateMap);
	      }catch(Exception ex)
	      {
	    	ex.printStackTrace();  
	      }
	      return count;
	    }
	    
	    
	    @Transactional(propagation = Propagation.REQUIRES_NEW)
	    public JSONObject checkSubscriptedMailExists(HttpServletRequest request) {
	        JSONObject resultObj = new JSONObject();
	        try {
	            String emailId = (String) request.getParameter("emailId");
	            String query = "SELECT EMAIL FROM S_PERS_DETAIL WHERE UPPER(EMAIL)=:EMAIL";
	            Map queryMap = new HashMap();
	            queryMap.put("EMAIL", emailId.toUpperCase());
	            List resultList = access.sqlqueryWithParams(query, queryMap);
	            if (resultList != null && !resultList.isEmpty()) {
	            	resultObj.put("status", false);
	            } else {
	                resultObj.put("status", true); 
	               }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	    
	    
	    @Transactional
		public JSONObject checkForCompanyAlreadyExist(HttpServletRequest request) {
			JSONObject resultObj = new JSONObject();
			try {
				String companyName = (String) request.getParameter("companyName");
				if (companyName != null && !"".equalsIgnoreCase(companyName)) {
					String getQuery = "SELECT NAME FROM ORGN_STRUCTURE WHERE UPPER(NAME)=:NAME";
					Map treeMap = new HashMap<>();
					treeMap.put("NAME", companyName.toUpperCase());
					List treeList = access.sqlqueryWithParams(getQuery, treeMap);
					if (treeList != null && !treeList.isEmpty()) { 
						String name = (String) treeList.get(0);
						if (name != null && !"".equalsIgnoreCase(name)) {
							name = name.toUpperCase();
							companyName = companyName.toUpperCase();
							if (name.equalsIgnoreCase(companyName)) {
								resultObj.put("status", false);
							} else {
								resultObj.put("status", true);
							}
							
						}
					} else {
						resultObj.put("status", true);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return resultObj;
		}

	@Transactional
	public Map<String, String> getDataTypesOFHeader(HttpServletRequest request) {
		Map<String ,String> resultMap = new HashMap<>();
		Connection connection = null;
		try{
			String tableName = request.getParameter("newTableName");
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM " + tableName + "");
			ResultSetMetaData metadata = results.getMetaData();
			int columnCount = metadata.getColumnCount();
			if (columnCount > 0) {

				for (int i = 1; i <= columnCount; i++) {
					String columnName = metadata.getColumnName(i);
					String columnType = metadata.getColumnTypeName(i);
					if("number".equalsIgnoreCase(columnType)  && columnType != null)
						resultMap.put(columnName, "number");
					else if("varchar2".equalsIgnoreCase(columnType)  && columnType != null)
						resultMap.put(columnName, "string");
					else if("date".equalsIgnoreCase(columnType)  && columnType != null)
						resultMap.put(columnName, "date");
					else
						resultMap.put(columnName,"string");

				}

			}


		}catch (Exception e){
			e.printStackTrace();

		}


		return resultMap;
	}

}
