/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pilog.mdm.Service;

import static java.lang.Integer.parseInt;
import static jxl.biff.BaseCellFeatures.logger;

import com.itextpdf.text.DocumentException;
import com.ccavenue.security.AesCryptUtil;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVReader;
import com.pilog.mdm.DAO.CloudGridResultsDAO;
import com.pilog.mdm.DAO.DashBoardsDAO;
import com.pilog.mdm.DTO.RegistrationDTO;
import com.pilog.mdm.Utils.DashBoardUtills;
import com.pilog.mdm.service.IntelliSenseRegistrationService;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.itextpdf.xmp.impl.Base64;
import com.pilog.mdm.utilities.PilogUtilities;
import java.sql.Clob;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import jxl.format.Colour;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;                            
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jagadish.K
 */
@Service
public class DashBoardsService { 

	@Autowired
	public DashBoardsDAO dashBoardsDAO; 
	
	@Autowired
	public IntelliSenseRegistrationService registrationService;

	@Value("${file.store.homedirectory}")
	private String fileStoreHomedirectory;
	
	@Value("${MultipartResolver.fileUploadSize}")
    private long maxFileSize;
	private int maxMemSize;
	
	private final static String CCAVENUE_WORKING_KEY_INR = "108F97CE68DEF1311DD7EA982E217D21";
    private final static String CCAVENUE_WORKING_KEY_NON_INR = "4072806B891169F4AF1DDB1D8964912C";

	@Autowired
	public CloudGridResultsDAO cloudGridResultsDAO;

	@Autowired
	public DashBoardUtills dashBoardUtills;

	private PilogUtilities cloudUtills = new PilogUtilities();
	
	 private static final String[] numNames = {
		        "",
		        " one",
		        " two",
		        " three",
		        " four",
		        " five",
		        " six",
		        " seven",
		        " eight",
		        " nine",
		        " ten",
		        " eleven",
		        " twelve",
		        " thirteen",
		        " fourteen",
		        " fifteen",
		        " sixteen",
		        " seventeen",
		        " eighteen",
		        " nineteen"
		    };
	 
	 private static final String[] tensNames = {
		        "",
		        " ten",
		        " twenty",
		        " thirty",
		        " forty",
		        " fifty",
		        " sixty",
		        " seventy",
		        " eighty",
		        " ninety"
		    };
	
	private String etlFilePath;
	{
		System.out.print("path :::"+System.getProperty("os.name"));
		if ((System.getProperty("os.name") !=null && !(System.getProperty("os.name").toUpperCase().startsWith("WINDOWS")))) {
			etlFilePath = "/u01/";
		} else {
			etlFilePath = "C://";
		}
	}

//    public String getVisualizationLayout(HttpServletRequest request) {
//        String result = "";
//        try {
//            result = "<div>"
//                    + " <div class=\"container-fluid\">"
//                    + "<div class=\"row\">"
//                    + "<div class=\"col-md-8 chartView\">"
//                    + "<div class=\"row\">"
//                    + " <div class=\"col-md-2 lefticonView\">"
//                    + "<div class=\"visionVisualizationDataSourcesCLass\" id=\"visionVisualizationDataSourcesId\">"
//                    + "<div id=\"VisualizationSources\" class =\"VisualizationSourcesCLass\" ></div> "
//                    + "</div>"
//                    + "</div>"
//                    + "<div class=\"col-md-10\" id=\"visualizeArea\">"
//                    + "<div class=\"visionVisualizationDataChartcount\" id=\"visionVisualizationDataChartcount\">"
//                    + "<div class=\"visionVisualizationDataChartViewCLass\" id=\"visionVisualizationDataChartViewId\">"
//                    + "</div>"
//                    + "</div>"
//                    + "</div>"
//                    + "</div>"
//                    + "</div>"
//                    + "<div class=\"col-md-4 formMainDIv\">"
//                    + "<div id=\"VisualizationFormAreaId\" class=\"VisualizationFormAreaClass\">"
//                    //                    + "<div class=\"VisionImageVisualizationFilterOpen\" style=\"display:none\"><span class=\"textSpanFilterCLass\">Filters</span><span class=\"imageSpanFiltersCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationFilterId\" class=\"VisionImageVisualizationFilterClass\" title=\"Show/Hide pane\"/></span></div>"
//                    //                    + "<div id =\"Filters\" class=\"VisionAnalyticsBIFilters\"><span>Filters</span><span class=\"imageSpanCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationFilter\" class=\"VisionImageVisualization\" title=\"Show/Hide pane\"/></span></div> "
//                    + "<div class=\"VisionImageVisualizationChartsOpen\" style=\"display:none\"><span  class=\"textSpanChartsCLass\">Visualizations</span><span class=\"imageSpanChartsCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationChartsId\" class=\"VisionImageVisualizationChartsClass\" title=\"Show/Hide pane\"/></span></div>"
//                    + "<div id =\"Visualization\" class=\"VisionAnalyticsBICharts\"><div><span>Visualizations</span><span class=\"imageSpanCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationCharts\" class=\"VisionImageVisualization\" title=\"Show/Hide pane\"/></span></div>"
//                    + "<div id=\"VisionBIVisualization\">"
//                    + "<div id='jqxTabCharts' class=\"jqxTabChartsClass\">"
//                    + "<div id='visionVisualizeBasicTabs' class='visionVisualizeChartsTabsClass'>"
//                    + "<img onclick=\"getChartDiv('Pie_Chart_Inner_Icon.svg', 'pie')\" src='images/Pie.svg' title='Pie chart'>"
//                    + "<img onclick=\"getChartDiv('Bar_Chart_Inner_Icon.svg', 'bar')\" src='images/Bar.svg' title='Bar chart'>"
//                    + "<img onclick=\"getChartDiv('Donut_Chart_Inner_Icon.svg', 'donut')\"  src='images/Donut.svg' title='Donut chart'>"
//                    + "<img onclick=\"getChartDiv('Column_Chart_Inner_Icon.svg', 'column')\"  src='images/Column.svg' title='Column chart'>"
//                    + "<img onclick=\"getChartDiv('Line_Chart_Inner_Icon.svg', 'lines')\"  src='images/Line.svg' title='Line chart'>"
//                    + "<img onclick=\"getChartDiv('Scatter_Chart_Inner_Icon.svg', 'scatter')\"  src='images/Scatter.svg' title='Scatter chart'>"
//                    + "<img onclick=\"getChartDiv('Tree_Chart_Inner_Icon.svg', 'treemap')\"  src='images/Tree_Chart.svg' title='Tree chart'>"
//                    + "<img onclick=\"getChartDiv('Histogram_Chart_Inner_Icon.svg', 'column')\"  src='images/Histogram.svg' title='Histogram chart'>"
//                    + "<img onclick=\"getChartDiv('Guage_Chart_Inner_Icon.svg', 'indicator')\"  src='images/Guage.svg' title='Guage chart'>"
//                    + "<img onclick=\"getChartDiv('Funnel_Chart_Inner_Icon.svg', 'funnel')\"  src='images/Funnel.svg' title='Funnel chart'>"
//                    + "<img onclick=\"getChartDiv('Candlestick_Chart_Inner_Icon.svg', 'candlestick')\"  src='images/Candlestick.svg' title='Candlestick chart'>"
//                    + "<img onclick=\"getChartDiv('Waterfall_Chart_Inner_Icon.svg', 'waterfall')\"  src='images/Waterfall.svg' title='Waterfall chart'>"
//                    + "<img onclick=\"getChartDiv('Redar-Chart-Thin.svg', 'scatterpolar')\"  src='images/Redar-Chart.svg' title='Radar chart'>"
//                    + "<img onclick=\"getChartDiv('vendorsCount.svg', 'Card')\"  src='images/Redar-Chart.svg' title='DashBordCard chart'>"
//                    + "</div>"
//                    + "</div>"
//                    + "<div id = 'visionVisualizeSlicerId' class='visionVisualizeSlicerClass'>"
//                    + "<div class='visionVisualizeSlicerImageDivClass'><img src=\"images/Chart_Slicer.svg\" onclick=\"showSlicerField('visionVisualizeSlicerFieldId')\" width=\"20px\" id=\"VisionVisualizationSlicerImageId\" class=\"VisionVisualizationSlicerImageClass\" title=\"Click for Slicer\"/></div>"
//                    + "<div id ='visionVisualizeSlicerFieldId' class='visionVisualizeSlicerFieldClass' style='display:none'><span>Drop Fields Here</span></div>"
//                    + "</div>"
//                    + "<div id='visualizeConfigTabs' class='visualizeConfigTabsClass'>"
//                    + "<ul id='visionVisualizeConfig'>"
//                    + "<li id='visionVisualizeFields' class='visionVisualizeFieldsClass'><img src='images/Fields_Selection.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigColumns','visionVisualizeFields')\"/></li>"
//                    + "<li id='visionVisualizeConfiguration' class='visionVisualizeConfigurationClass'><img src='images/Chart_Config.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigProperties','visionVisualizeConfiguration')\"/></li>"
//                    + "<li id='visionVisualizeFilters' class='visionVisualizeFiltersClass'><img src='images/Filter.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigFilters','visionVisualizeFilters')\"/></li>"
//                    + "<li id='visionVisualizeJoins' class='visionVisualizeJoinsClass'><img src='images/mapping.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigJoins','visionVisualizeJoins')\"/></li>"
//                    + "</ul>"
//                    + "</div>"
//                    + "<div id=\"visualizeChartConfigColumns\" class=\"visualizeChartConfigColumnsClass\"></div>"
//                    + "<div id=\"visualizeChartConfigProperties\" class=\"visualizeChartConfigPropertiesClass\" style='display:none'></div>"
//                    + "<div id=\"visualizeChartConfigFilters\" class=\"visualizeChartConfigFiltersClass\" style='display:none'></div>"
//                    + "<div id=\"visualizeChartConfigJoins\" class=\"visualizeChartConfigJoinsClass\" style='display:none'></div>"
//                    //                    + "</div>"
//                    + "</div>"
//                    + "</div>"
//                    + "<div class=\"VisionImageVisualizationFieldsOpen\" style=\"display:none\"><span  class=\"textSpanFieldsCLass\">Columns</span><span class=\"imageSpanFieldsCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationFieldsId\" class=\"VisionImageVisualizationFieldsClass\" title=\"Show/Hide pane\"/></span></div>"
//                    + "<div id =\"Fields\" class=\"VisionAnalyticsBIFields\"><div><span>Columns</span><span class=\"imageSpanCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationFields\" class=\"VisionImageVisualization\" title=\"Show/Hide pane\"/></span></div><div id=\"VisualizeBIColumns\"></div></div> "
//                    + "</div>"
//                    + "</div>"
//                    + "</div>"
//                    + "</div>"
//                    + "</div>";
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return result;
//    }
	public JSONObject getGenericDxpTreeOpt(HttpServletRequest request, String treeId) {
		JSONObject treeObj = new JSONObject();
		try {
			JSONObject labelsObj = new JSONObject(); 
			JSONObject treeConfigObj = new JSONObject();
			JSONObject treeInitParamObj = new JSONObject();
			JSONObject treeDefaultSource = new JSONObject();
			JSONObject treeColumnObj = new JSONObject();
			List<Object[]> treeList = dashBoardsDAO.getTreeListOpt(request, treeId);
			String treeGlobalSearchColsStr = "";

			if (treeList != null && !treeList.isEmpty()) {
				for (int i = 0; i < treeList.size(); i++) {
					Object[] treeObjArray = treeList.get(i);
					if (treeObjArray != null && treeObjArray.length != 0) {
						if (i == 0) {
							if (treeObjArray[2] != null && !"".equalsIgnoreCase(String.valueOf(treeObjArray[2]))
									&& !"null".equalsIgnoreCase(String.valueOf(treeObjArray[2]))) {
								treeObj.put("treeDesc", replaceSessionValues(String.valueOf(treeObjArray[2]), request));
							} else {
								treeObj.put("treeDesc", treeObjArray[2]);
							}

							treeConfigObj.put("width", treeObjArray[4]);
							treeConfigObj.put("height", treeObjArray[5]);
							treeConfigObj.put("enableHover", true);
							treeConfigObj.put("keyboardNavigation", true);
							treeConfigObj.put("incrementalSearch", true);
							treeConfigObj.put("theme", treeObjArray[3]);
							if (treeObjArray[6] != null && "CHBX".equalsIgnoreCase(String.valueOf(treeObjArray[6]))) {
								treeConfigObj.put("checkboxes", true);
								treeConfigObj.put("hasThreeStates", true);
							} else {
								treeConfigObj.put("checkboxes", false);
							}
							treeDefaultSource.put("label",
									((treeObjArray[8] != null && !"".equalsIgnoreCase(String.valueOf(treeObjArray[8]))
											&& !"null".equalsIgnoreCase(String.valueOf(treeObjArray[8])))
													? replaceSessionValues(String.valueOf(treeObjArray[8]), request)
													: treeObjArray[2]));
							treeDefaultSource.put("description",
									((treeObjArray[8] != null && !"".equalsIgnoreCase(String.valueOf(treeObjArray[8]))
											&& !"null".equalsIgnoreCase(String.valueOf(treeObjArray[8])))
													? replaceSessionValues(String.valueOf(treeObjArray[8]), request)
													: treeObjArray[2]));

							JSONArray sourceItems = new JSONArray();
							JSONObject itemObj = new JSONObject();
							itemObj.put("label",
									((treeObjArray[8] != null && !"".equalsIgnoreCase(String.valueOf(treeObjArray[8]))
											&& !"null".equalsIgnoreCase(String.valueOf(treeObjArray[8])))
													? replaceSessionValues(String.valueOf(treeObjArray[8]), request)
													: treeObjArray[2]));
							itemObj.put("value", "ajax");
							sourceItems.add(itemObj);
							treeDefaultSource.put("items", sourceItems);
							JSONObject treeInitParams = dashBoardsDAO
									.getInitParamObject(cloudUtills.clobToString((Clob) treeObjArray[19]));
							if (treeInitParams != null && !treeInitParams.isEmpty()
									&& treeInitParams.get("uuu_MultiTreeDlovId") != null
									&& !"".equalsIgnoreCase(String.valueOf(treeInitParams.get("uuu_MultiTreeDlovId")))
									&& !"null".equalsIgnoreCase(
											String.valueOf(treeInitParams.get("uuu_MultiTreeDlovId")))) {
								String buttonStr = "";
								String selectBoxStr = dashBoardsDAO.getLOV(request,
										(String) treeInitParams.get("uuu_MultiTreeDlovId"));
								String mainResultSearch = "<div class='mainsearch_input_div smartsearchtb'>";

								mainResultSearch += "<input type='text' id='mainTreeSearchResult' autocomplete='off' title='"
										+ cloudUtills.convertIntoMultilingualValue(
												labelsObj, "Enter atleast 3 characters to Search")
										+ "' " + "placeholder='  "
										+ cloudUtills.convertIntoMultilingualValue(labelsObj,
												"Type keyword(s) to search")
										+ "' "
										+ "data-no='NA' aria-haspopup='true' aria-multiline='false' aria-readonly='false' aria-disabled='false' aria-autocomplete='both' "
										+ "role='textbox' class='visionSearchClearResize clearable clearable2 ac jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic smartserachclass' "
										+ "data-selected='NO'>"
										+ "  <a class='clear_searchField' style='position: absolute; font-size: 18px; cursor: pointer; display: none; top:2.5px; right: 5px;' onclick='clearTextSearch();'>×</a>";

								mainResultSearch += "<div class='TreesearchButton'>"
										// + " <input type='submit' id='getsmartsearch' data-source='" + treeObjArray[1]
										// + "' class='searchbutton' value='' onclick=searchResultsHandler('s','lucene')
										// id='result' title='Click here to Search'>"
										+ "                                <input type='submit' id='getsmartsearch' data-source='"
										+ treeObjArray[1]
										+ "' class='dxpTreesearchbutton' value='' onclick=treeSearchResultsHandler('"
										+ treeObjArray[1] + "') id='result' title='Click here to Search'>"
										+ "            </div>"
										+ " <div data-selection-type='containing' data-text='NA' data-space='no' "
										+ "				class='smartsearchresultsviews' id='cloudTreeIntellisensebox' "
										+ "			        style='background: transparent none repeat scroll 0% 0%;'>"
										+ " <div class='cloudTreeSearchinnerclass' id='cloudTreeIntellisense'></div>"
										+ " <div id='cloudTreeIntellisense1'></div>" + "     </div>" + "</div>";
								treeObj.put("mainSearchBox", mainResultSearch);
								if (selectBoxStr != null && !"".equalsIgnoreCase(selectBoxStr)
										&& !"null".equalsIgnoreCase(selectBoxStr)) {
									selectBoxStr = "<span class='selectDxpBoxstr'>Selection Type: </span>"
											+ "<span class=\"treeDxpSelectBoxMain\"><select id=\"treeSelectBox\" onchange=getSelectedTree()>"
											+ selectBoxStr + "</select></span>";
									treeObj.put("selectBoxStr", selectBoxStr);
								}

							}
							String result = "";
							result += "<div class='treeSearchtextcount' id='treeSearchtextcount'></div></h3>";
							result += "<div class='search_input_div smartsearchtb' >";

							result += "<input type='text' id='dxptreeSearchResult' autocomplete='off' title='"
									+ cloudUtills.convertIntoMultilingualValue(labelsObj,
											"Enter atleast 3 characters to Search")
									+ "' " + "placeholder='  "
									+ cloudUtills.convertIntoMultilingualValue(labelsObj, "Type keyword(s) to search")
									+ "' "
									+ "data-no='NA' aria-haspopup='true' aria-multiline='false' aria-readonly='false' aria-disabled='false' aria-autocomplete='both' "
									+ "role='textbox' class='visionSearchClearResize clearable clearable2 ac jqx-widget-content jqx-widget-content-arctic jqx-input jqx-input-arctic jqx-widget jqx-widget-arctic jqx-rc-all jqx-rc-all-arctic smartserachclass' "
									+ "data-selected='NO'>"
									+ "  <a class='clear_searchField' style='position: absolute; font-size: 18px; cursor: pointer; display: none; top:2.5px; right: 5px;' onclick='clearTextSearch();'>×</a>";

							result += "</div><div class='TreesearchButton'>"
									+ "                                <input type='submit' id='getsmartsearch' data-source='"
									+ treeObjArray[1]
									+ "' class='dxpTreesearchbutton' value='' onclick=treeSearchResultsHandler('"
									+ treeObjArray[1] + "') id='result' title='Click here to Search'>"
									+ "            </div>";
							result += "<div data-selection-type='containing' data-text='NA' data-space='no' "
									+ "				class='dxpTreesmartsearchresults' id='intellisenseTreebox' "
									+ "			        style='background: transparent none repeat scroll 0% 0%;'>"
									+ " <div class='dxpTreesearchinnerclass' id='intellisenseTree'></div>"
									+ " <div id='dxpTreeintellisense'></div>" + "     </div>";

							treeObj.put("searchField", result);
						}
						JSONObject columnObj = new JSONObject();
						columnObj.put("TREE_REF_TABLE", treeObjArray[1]);// TREE_REF_TABLE
						columnObj.put("HL_FLD_NAME", treeObjArray[13]);// HL_FLD_NAME
						columnObj.put("FLD_NAME", treeObjArray[14]);// FLD_NAME
						columnObj.put("DISP_FLD_NAME", treeObjArray[15]);// DISP_FLD_NAME
						columnObj.put("FOLLOWUP_COMP_ID", treeObjArray[16]);// FOLLOWUP_COMP_ID
						columnObj.put("FOLLOWUP_COMP_TYPE", treeObjArray[17]);// FOLLOWUP_COMP_TYPE
						columnObj.put("FOLLOWOP_COMP_DESCR", treeObjArray[18]);// FOLLOWOP_COMP_DESCR

						if (treeObjArray[10] != null && !"".equalsIgnoreCase(String.valueOf(treeObjArray[10]))
								&& !"null".equalsIgnoreCase(String.valueOf(treeObjArray[10]))) {
							columnObj.put("TREE_PARAMS_ID", treeObjArray[10]);// TREE_PARAMS_ID
						}
						if (treeObjArray[19] != null && !"".equalsIgnoreCase(String.valueOf(treeObjArray[19]))
								&& !"null".equalsIgnoreCase(String.valueOf(treeObjArray[19]))) {
							columnObj.put("TREE_INIT_PARAMS", dashBoardsDAO
									.getInitParamObject(cloudUtills.clobToString((Clob) treeObjArray[19])));// TREE_INIT_PARAMS
						}
						treeColumnObj.put(i, columnObj);
					}
					JSONObject searchTreeInitParams = dashBoardsDAO
							.getInitParamObject(cloudUtills.clobToString((Clob) treeObjArray[19]));
					if (searchTreeInitParams != null && !searchTreeInitParams.isEmpty()
							&& searchTreeInitParams.get("uuu_GlobalTreeSearchColEnable") != null
							&& !"".equalsIgnoreCase(
									String.valueOf(searchTreeInitParams.get("uuu_GlobalTreeSearchColEnable")))
							&& "Y".equalsIgnoreCase(
									String.valueOf(searchTreeInitParams.get("uuu_GlobalTreeSearchColEnable")))) {
						treeGlobalSearchColsStr = "" + treeObjArray[34] + "";
						treeObj.put("treeGlobalSearchColumns", treeGlobalSearchColsStr);
						treeObj.put("FOLLOWUP_COMP_ID", treeObjArray[16]);

					}
				}
			}
			String divId = " <div class=\"mainDxpSplitter\" id=\"mainDxpSplitter\">"
					+ "<div class=\"firstDxpSplitterTree\" id=\"firstDxpSplitterTree\" style=\"overflow-y: auto;\">"
					+ " <div class=\"firstDxpSplitterData\" id=\"firstDxpSplitterData\">" + "<div id='jqxExpander'>"
					+ " <div id=\"expanderDesc\" class=\"visionTreeDescription\">" + treeObj.get("treeDesc") + "</div>"
					+ "<div style=\"border: none;\" id='jqxTreeDropdown' class=\"visionDxpTreeDropDown\">"
					+ treeObj.get("selectBoxStr") + "</div>"
					+ " <div style=\"border: none;\" id='jqxTreeDropdown' class=\"visionTreeSearchResults\" >"
					+ treeObj.get("searchField") + "</div>"
					+ "<div style=\"overflow: hidden;\" id=\"jqxTreeDiv\" class=\"visionjqxTreeDiv\">"
					+ "<div style=\"border: none;\" id='jqxTree'></div></div>" + "</div>" + "</div>"
					+ "<div id=\"treeGridDiv\" class=\"visionTreeCompDiv\"></div>" + "</div>" + "</div>";
			JSONArray treeDefaultSourceArray = new JSONArray();
			treeDefaultSourceArray.add(treeDefaultSource);
			treeConfigObj.put("source", treeDefaultSourceArray);
			treeObj.put("treeConfigObj", treeConfigObj);
			treeObj.put("treeColumnObj", treeColumnObj);
			treeObj.put("divid", divId);
		} catch (Exception e) {
		}
		return treeObj;
	}

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

	public String getVisualizationLayout(HttpServletRequest request) {
		String result = "";
		String Connection = "Test";
		try {
			String treeId = request.getParameter("treeId");
			JSONObject treeObj = getGenericDxpTreeOpt(request, treeId);             

			result = "<div class='dxpDataAnalyticswrapper'>"
					+ "<div class='leftFileUploads width60' id=\"leftFileUploadMainDivwrapperID\">"
					+ "<div class=\"leftUploadHeaderDiv\" onclick=\"leftFileUploadsDivToggle()\">"
					+ "<span class=\"uploadstitle\">" + "<h4>Data&nbsp;Integration</h4>" + "</span>"
					+ "<span class=\"toggleImg\" id=\"columnsToggleIcon\" onclick=\\\"dataIntegrationGuide()\\\"><img src=\"images/toggle_plusicon.png\" width=\"16px;\">"
					+ "</span>" + "</div>"
					// + "<div id=\"savedConnections\" style='display:none' >\n"
					+ "<div id='dBConnection' class='dBConnectionClass'>" + "<ul id='dxpConnection'>"
					+ "<li title='New Connections' id='visualConnectionLi' >"
					+ "<img src='images/New Connection Icon-01.svg' onclick=showVisualizationConnection() class='visionEtlTabIcons visualDarkMode' style='cursor:pointer;'>"
					+ "</li>" + "<li title='Available Connections' id='treeDxpConnectionLi' >"
					+ "<img src='images/tree.svg' onclick=treeDxpConnections() class='visionEtlTabIcons visualDarkMode' style='cursor:pointer;'>"
					+ "</li>" + "</ul>" + "</div>"
					+ "<div id=\"ivisualizationConnectionsMain\" class='ivisualizationConnectionsMainClass' style='display:none' >"
					+ "<div id=\"savedConnIcons\" class=\"savedConnIconsClass\">"
					+ "<img src=\"images/Refresh Icon.svg\" class=\"visionETLIcons visualDarkMode\" title=\"Refresh\" style=\"width:15px;height: 15px;cursor:pointer;\" "
					+ "onclick='refreshMappingTablesAnalytics()'/>"
					+ "<img src=\"images/Filter Icon-01.svg\" class=\"visionETLIcons visualDarkMode\" id=\"treeETLFilterImage\" title=\"Filter\" style=\"width:15px;height: 15px;cursor:pointer;\""
					+ "onclick='filterMappingTablesAnalytics()'/>" + "</div>"
					+ "<div id=\"ivisualizationConnections\" class='ivisualizationConnectionsClass'>" + "</div>"
					+ "</div>"
					// + "<div id=\"ivisualizationConnections\"
					// class='ivisualizationConnectionsClass' style='display:none' >"
					// +"</div>"
					+ "<div class='visionVisualizationDataSourcesCLass fileUploadsDA' id='visionVisualizationDataSourcesId'>"
					+ "<div id=\"VisualizationSources\" class ='VisualizationSourcesCLass'></div> "
					// + "<div id=\"savedConnections\" style='display:none' ></div>"
					+ "</div>" + "</div>"
					+ "<div id=\"visualizationMainDivwrapperID\" class='visualizationMainDivwrapper width60'>"
					+ "<div class=\"visualizationHeaderDiv\" onclick=\"visualizationDivToggle()\">"
					+ "<span class=\"visualizationtitle\">" + "<h4>Data&nbsp;Analytics</h4>" + "</span>"
					+ "<span class=\"toggleImg\" id=\"visualToggleIcon\" onclick=\\\"dataAnalyticGuide()\\\"><img src=\"images/toggle_plusicon.png\" width=\"16px;\" class='visualDarkMode'></span>"
					+ "</div>" + "<div id =\"Visualization\" class='VisionAnalyticsBICharts visualBIChart'>"
					+ "<div id=\"VisionBIVisualization\">" + "<div id='jqxTabCharts' class=\"jqxTabChartsClass\">"
					+ "<div id='visionVisualizeBasicTabs' class='visionVisualizeChartsTabsClass'>"
					+ "<div class='row iconsRow'>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Pie_Chart_Inner_Icon.svg', 'pie')\" src='images/Pie.svg' class='visualDarkMode' title='Pie chart looks like circle it is divided into sectors that each represent a proportion of the whole.'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Bar_Chart_Inner_Icon.svg', 'bar')\" src='images/Bar.svg' class='visualDarkMode' title='A bar chart is a chart that presents categorical data with rectangular bars with lengths proportional to the values that they represent. The bars can be plotted horizontally'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Donut_Chart_Inner_Icon.svg', 'donut')\"  src='images/Donut.svg' class='visualDarkMode' title='Doughnut chart looks like circle with hole it is divided into sectors that each represent a proportion of the whole'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Column_Chart_Inner_Icon.svg', 'column')\"  src='images/Column.svg' class='visualDarkMode' title='A column chart is a chart that presents categorical data with rectangular bars with heights proportional to the values that they represent. The bars can be plotted vertically'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Line_Chart_Inner_Icon.svg', 'lines')\"  src='images/Line.svg' class='visualDarkMode' title='A line chart is a type of chart which displays information as a series of data points called 'markers' connected by straight line segments'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Scatter_Chart_Inner_Icon.svg', 'scatter')\"  src='images/Scatter.svg' class='visualDarkMode' title='Scatter chart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Tree_Chart_Inner_Icon.svg', 'treemap')\"  src='images/Tree_Chart.svg' class='visualDarkMode' title='Tree maps display hierarchical data as a set of nested rectangles. Each branch of the tree is given a rectangle, which is then tiled with smaller rectangles representing sub-branches'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Histogram_Chart_Inner_Icon.svg', 'column')\"  src='images/Histogram.svg' class='visualDarkMode' title='Histogram chart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Guage_Chart_Inner_Icon.svg', 'indicator')\"  src='images/Guage.svg' class='visualDarkMode' title='Guage chart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Funnel_Chart_Inner_Icon.svg', 'funnel')\"  src='images/Funnel.svg' class='visualDarkMode' title='Funnel charts can be used to illustrate stages in a process, they could be used to show anything that’s decreasing in size'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Candlestick_Chart_Inner_Icon.svg', 'candlestick')\"  src='images/Candlestick.svg' class='visualDarkMode' title='Candlestick chart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Waterfall_Chart_Inner_Icon.svg', 'waterfall')\"  src='images/Waterfall.svg' class='visualDarkMode' title='A waterfall chart is a form of data visualization that helps in understanding the cumulative effect of sequentially introduced positive or negative values. These intermediate values can either be time based or category based'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Redar-Chart-Thin.svg', 'scatterpolar')\"  src='images/Redar-Chart.svg' class='visualDarkMode' title='A radar chart is a graphical method of displaying multivariate data in the form of a two-dimensional chart of three or more quantitative variables represented on axes starting from the same point'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('HeatMap_Inner_Icon.svg', 'heatMap')\"  src='images/HeatMap.svg' class='visualDarkMode' title='A heat map is a data visualization technique that shows magnitude of a phenomenon as color in two dimensions. The variation in color may be by hue or intensity, giving obvious visual cues to the reader about how the phenomenon is clustered or varies over space'></div>"
					//+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Bar_Chart_Inner_Icon.svg', 'barRotation')\" src='images/Bar.svg' class='visualDarkMode' title='Bar Label Rotation chart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Sunburst_Inner_Icon.svg', 'sunburst')\" src='images/Sunburst.svg' class='visualDarkMode' title='The sunburst chart is ideal for displaying hierarchical data. Each level of the hierarchy is represented by one ring or circle with the innermost circle as the top of the hierarchy'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('GeoChart_Inner_Icon.svg', 'geochart')\" src='images/GeoChart.svg' class='visualDarkMode' title='Geo chart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Bar_Chart_Inner_Icon.svg', 'BarAndLine')\" src='images/Bar_Chart_Inner_Icon.svg' class='visualDarkMode' title='Bar and Line chart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Integral-Analytics-Icon.png', 'boxplot')\" src='images/Integral-Analytics-Icon.png' class='visualDarkMode' title='Box Plot'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Sunburst_Inner_Icon.svg', 'sankey')\" src='images/Sunburst.svg' class='visualDarkMode' title='The sankey chart is ideal for displaying hierarchical data. Each level of the hierarchy is represented by one ring or circle with the innermost circle as the top of the hierarchy'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('BasicAreaChart.png', 'BasicAreaChart')\" src='images/BasicAreaChart.png' class='visualDarkMode' title='BasicAreaChart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('StackedAreaChart.png', 'StackedAreaChart')\" src='images/StackedAreaChart.png' class='visualDarkMode' title='StackedAreaChart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('GradientStackedAreaChart.png', 'GradStackAreaChart')\" src='images/GradientStackedAreaChart.png' class='visualDarkMode' title='GradientStackedAreaChart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('AreaPiecesChart.png', 'AreaPiecesChart')\" src='images/AreaPiecesChart.png' class='visualDarkMode' title='AreaPiecesChart'></div>"
					+ "<div class='col-lg-4  col-md-4 visualIconDivImg'><img onclick=\"getChartDiv('Gantt-chart.png', 'ganttChart')\" src='images/Gantt-chart.png' class='visualDarkMode' title='GanttChart'></div>"

					+ "</div>" + "</div>" + "</div>"
					+ "<div id='visionVisualizeCardTypesId' class='visionVisualizeCardTypesClass'>"
					+ "<span class='visionVisualizeCardTypesSpanClass'>Cards :</span>"
					+ "<img src='images/DashBoardCard.svg' class='visualDarkMode' style='cursor:pointer;' width=\"20px\" title='Card' onclick=\"getChartDiv('DashBoardCard.svg','Card','','','Normal')\"/>"
					+ "<img src='images/DashBoardCard.svg' class='visualCardDarkMode' style='cursor:pointer;' width=\"20px\" onclick=\"getChartDiv('DashBoardCard.svg','Card','','','Rectangle')\"/>"
					+ "</div>" + "<div id = 'visionVisualizeSlicerId' class='visionVisualizeSlicerClass'>"
					+ "<div class='visionVisualizeSlicerImageDivClass'><img src=\"images/Chart_Slicer.svg\" onclick=\"showSlicerField('visionVisualizeSlicerFieldId')\" width=\"20px\" id=\"VisionVisualizationSlicerImageId\" class=\"VisionVisualizationSlicerImageClass visualDarkMode\" title=\"Click for Slicer\"/></div>"
					+ "<div id ='visionVisualizeSlicerFieldId' class='visionVisualizeSlicerFieldClass' style='display:none'><span>Drop Fields Here</span></div>"
					+ "</div>" + "<div id='visualizeConfigTabs' class='visualizeConfigTabsClass'>"
					+ "<ul id='visionVisualizeConfig'>"
					+ "<li id='visionVisualizeFields' class='visionVisualizeFieldsClass'><img src='images/Fields_Selection.svg' class='visualDarkMode' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigColumns','visionVisualizeFields')\"/></li>"
					+ "<li id='visionVisualizeConfiguration' class='visionVisualizeConfigurationClass'><img src='images/Chart_Config.svg' class='visualDarkMode' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigProperties','visionVisualizeConfiguration')\"/></li>"
					+ "<li id='visionVisualizeFilters' class='visionVisualizeFiltersClass'><img src='images/Filter.svg' class='visualDarkMode' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigFilters','visionVisualizeFilters')\"/></li>"
					+ "<li id='visionVisualizeJoins' class='visionVisualizeJoinsClass'><img src='images/mapping.svg' class='visualDarkMode' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigJoins','visionVisualizeJoins')\"/></li>"
					+ "</ul>" + "</div>"
					+ "<div id=\"visualizeChartConfigColumns\" class=\"visualizeChartConfigColumnsClass\"></div>"
					+ "<div id=\"visualizeChartConfigProperties\" class=\"visualizeChartConfigPropertiesClass\" style='display:none'></div>"
					+ "<div id=\"visualizeChartConfigFilters\" class=\"visualizeChartConfigFiltersClass\" style='display:none'></div>"
					+ "<div id=\"visualizeChartConfigJoins\" class=\"visualizeChartConfigJoinsClass\" style='display:none'></div>"
					// + "</div>"
					+ "</div>" + "</div>" + "</div>"
					+ "<div class=\"chartViewAreaClass\" id=\"visualizeChartAndDataArea\">"
					+ "<div class=\"chartView\" id=\"visualizeArea\">"
					+ "<div class=\"visionVisualizationDataChartcount\" id=\"visionVisualizationDataChartcount\">"
					+ "<div class=\"visionVisualizationDataChartViewFilterClass\" id=\"visionVisualizationDataChartViewFilterId\" style='display:none'>"
					+ "<div class='visionHomeAutoSuggestionChartCount'><span class='visionAutoSuggestionChartCountSpanClass'>Charts Count :</span><span class='visionAutoSuggestionChartCountSpan'></span></div>"
					+ "<div id=\"visionHomeChartSuggestionsSaveId\" class=\"visionHomeChartSuggestionsFilterClass\"><span class=\"FilterImage\"><img onclick=\"saveHomePageAutoSuggestionsCharts()\" src=\"images/Save Icon.svg\" title=\"Save Charts\" style=\"width:20px;margin-left: 7px;\"></span></div>"
					+ "<div id=\"visionHomeChartSuggestionsFilterId\" class=\"visionHomeChartSuggestionsFilterClass\"><span class=\"FilterImage\"><img onclick=\"filterHomePageAutoSuggestionsCharts()\" src=\"images/filter.png\" title=\"Filter Charts\" style=\"width:20px;margin-left: 7px;\"></span></div>"
					+ "<div id=\"visionHomeChartSuggestionsDeleteId\" class=\"visionHomeChartSuggestionsFilterClass\"><span class=\"FilterImage\"><img onclick=\"deleteHomePageAutoSuggestionsCharts()\" src=\"images/delete_icon.svg\" title=\"Delete Charts\" style=\"width:20px;margin-left: 7px;\"></span></div>"
					+ "<div id='visionVisualizeChartsInRowId' class='visionVisualizeChartsInRowClass'>"
					+ "<span class='visionVisualizeChartsInRowSpanClass'>Charts in Row :</span>"
					+ "<select id='visionVisualizeChartsInRowSelectId' onchange='showChartsInRow()'>"
					+ "<option value='2'>2</option>" + "<option value='3' selected>3</option>"
					+ "<option value='4'>4</option>" + "</select>" + "</div>" 
					+"<div id='visionVisualizeChartsBasedOnQuestionsMainId' class='visionVisualizeChartsBasedOnQuestionsMainClass'>"
					+"<div id='visionVisualizeChartsBasedOnQuestionsImageId' class='visionVisualizeChartsBasedOnQuestionsImageClass'><img src='./images/questions-img.png'></div>"
					+"<div id='visionVisualizeChartsBasedOnQuestionsId' class='visionVisualizeChartsBasedOnQuestionsClass'></div>"
					+"</div>"
					+ "</div>"
					+ "<div class=\"visionVisualizationDataChartViewCLass\" id=\"visionVisualizationDataChartViewId\"></div>"
					+ "<div class=\"visionVisualizationDataModalChartViewCLass container-fluid\" id=\"visionVisualizationDataModalChartViewId\"></div></div>"
					+ "</div>" + "<div class=\"dataView\" id=\"visionGridDataView\" style=\"display:none\">"
					+ "<div class=\"VisionImageVisualizationFieldsOpen\" style=\"display:none\"><span  class=\"textSpanFieldsCLass\">Columns</span><span class=\"imageSpanFieldsCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationFieldsId\" class=\"VisionImageVisualizationFieldsClass\" title=\"Show/Hide pane\"/></span></div>"
					+ "<div id =\"Fields\" class=\"VisionAnalyticsBIFields\" style=\"display:none\"><div><span>Columns</span><span class=\"imageSpanCLass\"><img src=\"images/nextrightarrow.png\" width=\"16px\" id=\"VisionImageVisualizationFields\" class=\"VisionImageVisualization\" title=\"Show/Hide pane\"/></span></div><div id=\"VisualizeBIColumns\"></div></div> "
					+ "<div class=\"visionSmartBiGridDataClass\" id=\"visionSmartBiGridDataId\">"
					+ "<div class=\"buttoonClass\" id=\"btnGroup\" style=\"display:none\"></div>"
					+ "<div class=\"gridDataView\" id=\"visualizeAreaGirdData1\"></div>"
					+ "<div class=\"visualizeAreaGirdDataView\" id=\"visualizeAreaGirdData\" style='display:none'></div>"
					+ "</div>"
					+ "<div class=\"visualizeTablesGridDataView\" id=\"visualizeTablesGridData\" style='display:none'></div>"
					+ "</div>"
					+ "<div id='visionChartAutoSuggestionsViewId' class='visionChartAutoSuggestionsViewClass' style=\"display:none\">"
					+ "<div class='visionChartsUserAutoSuggetionsClass' id='visionChartsUserAutoSuggetionsClassId'>"
					+ "<div class='visionChartsAutoSuggestionUserClass' id='visionChartsAutoSuggestionUserId1'></div>"
					+ "<div class='visionChartsAutoSuggestionUserExamplesClass' id='visionChartsAutoSuggestionUserExamplesId'>"
					+ "<div id='visionInsightsVisualizationChartId' class='visionInsightsVisualizationChartClass'></div>"
					+ "<div id='visionInsightsVisualizationChartDataId' class='visionInsightsVisualizationChartDataClass'></div>"
					+ "</div>"
					+ "</div>" + "</div>"
					+ "<div id='visionVisualizeQueryGridId' class='visionVisualizeQueryGridClass' style=\"display:none\">"
					+ "<div id='visionVisualizeQueryId' class='visionVisualizeQueryClass'>"
					+ "<div id='visionVisualizeQueryHeaderId' class='visionVisualizeQueryHeaderClass'>"
					+ "<img id=\"scriptsExecute\" onclick=\"executeBIEditorScripts('visionVisualizeQueryBodyId')\" src=\"images/oracle_db.png\" class=\"visionETLIcons visionIntegralEditorIcons\" style=\"width:18px;height: 18px;cursor:pointer;\">"
					+ "<img id=\"pythonScriptsExecute\" onclick=\"executePythonBIEditorScripts('visionVisualizeQueryBodyId')\" src=\"images/python.png\" class=\"visionETLIcons visionIntegralEditorIcons\" style=\"width:18px;height: 18px;cursor:pointer;\">"
					+ "<img id=\"mergeTablesId\" onclick=\"mergeTables('visionVisualizeQueryBodyId')\" src=\"images/merge.png\" class=\"visionETLIcons visionIntegralEditorIcons\" style=\"width:18px;height: 18px;cursor:pointer;\">"
					+ "<img id=\"tablesSearchId\" onclick=\"searchTablesAi()\"  class=\"visionETLSearchIcons visionIntegralEditorIcons\" src=\"images/image_2023_03_27T10_09_03_182R.png\" style=\"width:18px;height: 18px;cursor:pointer;\">"
					+ "</div>"
					+ "<div id='visionVisualizeQueryTablesBodyParentId' class='visionVisualizeQueryTablesBodyParentClass'>"
					+"<div id='visionVisualizeQueryBodyParentId' class='visionVisualizeQueryBodyParentClass'>"
					+ "<div id='visionVisualizeQueryBodyId' class='visionVisualizeQueryBodyClass'>"
					+ "<div id='Current_V10_editor_1' class='Current_V10_editor_1Class'></div>" 
					+ "</div>" 
					+ "<div id=\"searchDataContent\" class=\"searchDataContentClass\" style=\"display:none\"></div>"
					+ "</div>"
					+"<div id='visionVisualizeShowTablesDataId' class='visionVisualizeShowTablesDataClass'>"
					+"</div>"
					+"</div>"
					+ "</div>"
					+ "<div id='visionVisualizeQueryGridDataId' class='visionVisualizeQueryGridDataClass'>"
					+ "<div id='visionVisualizeQueryGridButtonsId' class='visionVisualizeQueryGridButtonsClass'></div>"
					+ "<div id='visionVisualizeQueryGridDataBodyId' class='visionVisualizeQueryGridDataBodyClass'></div>"
					+ "</div>" + "</div>"
					+ "<div id=\"designViewTabHeading\" class=\"visionSmartBIDesignTabHeadingsDiv\">"
					+ "<ul class=\"visionSmartBIDesignTabHeadings\">"
					+ "<li title=\"Design View\" id=\"li_designView\" class=\"visionSmartBiDesignTab visionSmartBiDesignTabHighLight\" onclick=\"switchSmartBiDesignTabs('li_designView', 'visualizeArea')\" ><span>Design View</span></li>\n"
					+ "<li title=\"Data View\" id=\"li_contentView\" class=\"visionSmartBiDesignTab\"onclick=\"switchSmartBiDesignTabs('li_contentView', 'visionGridDataView')\"><span>Data View</span></li> \n"
					+ "<li title=\"IntelliSense View\" id=\"li_autoSuggestionsView\" class=\"visionSmartBiDesignTab\"onclick=\"switchSmartBiDesignTabs('li_autoSuggestionsView', 'visionChartAutoSuggestionsViewId')\"><span>Insights View</span></li> \n"
					+ "<li title=\"Editor View\" id=\"li_queryGridView\" class=\"visionSmartBiDesignTab\"onclick=\"switchSmartBiDesignTabs('li_queryGridView', 'visionVisualizeQueryGridId')\"><span>Editor View</span></li> \n"
					+ "</ul>" + "</div>" + "</div>" + "<div id='dialog'></div>" + "<div id='dxpCreatePopOver'></div>"
					+ "<div id ='drillDownChartDataDialog'></div>"
					+ "<div id ='visionVisualizeChartEditAISuggest'></div>";
		} catch (Exception ex) { 
			ex.printStackTrace();
		}
		return result;
	}

	public JSONArray getDataMigrationConnectionsTreeMenu(HttpServletRequest request, String parentMenuId) {

		JSONArray menuArray = new JSONArray();
		try {
			if (parentMenuId != null) {    
				JSONObject menuObj = new JSONObject();   
				menuObj.put("id", "FILES");
				menuObj.put("PARENT_ID", parentMenuId);
				menuObj.put("PARENT_MENU_ID", parentMenuId);
				menuObj.put("MENU_ID", "FILES");
				menuObj.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>Files</span>");
				menuObj.put("MAIN_DESCRIPTION", "Files");
				menuObj.put("icon", "images/File-Icon.svg");
				menuObj.put("value", "javascript:getTreeDataBase()");
				menuObj.put("TOOL_TIP", "Files");
				menuArray.add(menuObj);
				JSONObject menuObj1 = new JSONObject();
				menuObj1.put("id", "DATABASE");
				menuObj1.put("PARENT_ID", parentMenuId);
				menuObj1.put("PARENT_MENU_ID", parentMenuId);
				menuObj1.put("MENU_ID", "DATABASE");
				menuObj1.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>Database</span>");
				menuObj1.put("MAIN_DESCRIPTION", "Database");
				menuObj1.put("icon", "images/DB-Icon.svg");
				menuObj1.put("value", "javascript:getTreeDataBase()");
				menuObj1.put("TOOL_TIP", "Database");
				menuArray.add(menuObj1);
				JSONObject menuObj2 = new JSONObject();
				menuObj2.put("id", "ONL_SERVICES");
				menuObj2.put("PARENT_ID", parentMenuId);
				menuObj2.put("PARENT_MENU_ID", parentMenuId);
				menuObj2.put("MENU_ID", "ONL_SERVICES");
				menuObj2.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>Online Services</span>");
				menuObj2.put("MAIN_DESCRIPTION", "Online Services");
				menuObj2.put("icon", "images/ONLINE_SERVICES_Icon.svg");
				menuObj2.put("value", "javascript:getTreeDataBase()");
				menuObj2.put("TOOL_TIP", "Online Services");
				menuArray.add(menuObj2);
				JSONObject menuObj3 = new JSONObject();
				menuObj3.put("id", "ERP");
				menuObj3.put("PARENT_ID", parentMenuId);
				menuObj3.put("PARENT_MENU_ID", parentMenuId);
				menuObj3.put("MENU_ID", "ERP");
				menuObj3.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>ERP</span>");
				menuObj3.put("MAIN_DESCRIPTION", "ERP");
				menuObj3.put("icon", "images/ERP-Icon.svg");
				menuObj3.put("value", "javascript:getTreeDataBase()");
				menuObj3.put("TOOL_TIP", "ERP");
				menuArray.add(menuObj3);
				JSONObject menuObj5 = new JSONObject();
				menuObj5.put("id", "CSV");
				menuObj5.put("PARENT_ID", "FILES");
				menuObj5.put("PARENT_MENU_ID", "FILES");
				menuObj5.put("MENU_ID", "CSV");
				menuObj5.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>CSV</span>");
				menuObj5.put("MAIN_DESCRIPTION", "CSV");
				menuObj5.put("icon", "images/CSV-Icon.svg");
				menuObj5.put("value", "javascript:anlyticsgetTreeDataBase('FILE','CSV')");
				menuObj5.put("TOOL_TIP", "CSV");
				menuArray.add(menuObj5);
				JSONObject menuObj6 = new JSONObject();
				menuObj6.put("id", "XLS");
				menuObj6.put("PARENT_ID", "FILES");
				menuObj6.put("PARENT_MENU_ID", "FILES");
				menuObj6.put("MENU_ID", "XLS");
				menuObj6.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>XLS</span>");
				menuObj6.put("MAIN_DESCRIPTION", "XLS");
				menuObj6.put("icon", "images/xls-Icon.svg");
				menuObj6.put("value", "javascript:anlyticsgetTreeDataBase('FILE','XLS')");
				menuObj6.put("TOOL_TIP", "XLS");
				menuArray.add(menuObj6);
				JSONObject menuObj7 = new JSONObject();
				menuObj7.put("id", "XLSX");
				menuObj7.put("PARENT_ID", "FILES");
				menuObj7.put("PARENT_MENU_ID", "FILES");
				menuObj7.put("MENU_ID", "XLSX");
				menuObj7.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>XLSX</span>");
				menuObj7.put("MAIN_DESCRIPTION", "XLSX");
				menuObj7.put("icon", "images/XLSX-Icon.svg");
				menuObj7.put("value", "javascript:anlyticsgetTreeDataBase('FILE','XLSX')");
				menuObj7.put("TOOL_TIP", "XLSX");
				menuArray.add(menuObj7);
				JSONObject menuObj8 = new JSONObject();
				menuObj8.put("id", "JSON");
				menuObj8.put("PARENT_ID", "FILES");
				menuObj8.put("PARENT_MENU_ID", "FILES");
				menuObj8.put("MENU_ID", "JSON");
				menuObj8.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>JSON</span>");
				menuObj8.put("MAIN_DESCRIPTION", "JSON");
				menuObj8.put("icon", "images/JSON_Icon.svg");
				menuObj8.put("value", "javascript:anlyticsgetTreeDataBase('FILE','JSON')");
				menuObj8.put("TOOL_TIP", "JSON");
				menuArray.add(menuObj8);
				JSONObject menuObj9 = new JSONObject();
				menuObj9.put("id", "XML");
				menuObj9.put("PARENT_ID", "FILES");
				menuObj9.put("PARENT_MENU_ID", "FILES");
				menuObj9.put("MENU_ID", "XML");
				menuObj9.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>XML</span>");
				menuObj9.put("MAIN_DESCRIPTION", "XML");
				menuObj9.put("icon", "images/XML-Icon.svg");
				menuObj9.put("value", "javascript:anlyticsgetTreeDataBase('FILE','XML')");
				menuObj9.put("TOOL_TIP", "XML");
				menuArray.add(menuObj9);
				JSONObject menuObj10 = new JSONObject();
				menuObj10.put("id", "TEXT");
				menuObj10.put("PARENT_ID", "FILES");
				menuObj10.put("PARENT_MENU_ID", "FILES");
				menuObj10.put("MENU_ID", "TEXT");
				menuObj10.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>TEXT</span>");
				menuObj10.put("MAIN_DESCRIPTION", "TEXT");
				menuObj10.put("icon", "images/TEXT_Icon.svg");
				menuObj10.put("value", "javascript:anlyticsgetTreeDataBase('FILE','TEXT')");
				menuObj10.put("TOOL_TIP", "TEXT");
				menuArray.add(menuObj10);
				JSONObject menuObj11 = new JSONObject();
				menuObj11.put("id", "ORACLE");
				menuObj11.put("PARENT_ID", "DATABASE");
				menuObj11.put("PARENT_MENU_ID", "DATABASE");
				menuObj11.put("MENU_ID", "ORACLE");
				menuObj11.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>Oracle</span>");
				menuObj11.put("MAIN_DESCRIPTION", "Oracle");
				menuObj11.put("icon", "images/DM_ORACLE-Icon.svg");
				menuObj11.put("value", "javascript:getTreeDataBase('DB','Oracle')");
				menuObj11.put("TOOL_TIP", "Oracle");
				menuArray.add(menuObj11);
				JSONObject menuObj12 = new JSONObject();
				menuObj12.put("id", "MYSQL");
				menuObj12.put("PARENT_ID", "DATABASE");
				menuObj12.put("PARENT_MENU_ID", "DATABASE");
				menuObj12.put("MENU_ID", "MYSQL");
				menuObj12.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>MYSQL</span>");
				menuObj12.put("MAIN_DESCRIPTION", "MYSQL");
				menuObj12.put("icon", "images/MYSQL_Icon.png");
				menuObj12.put("value", "javascript:getTreeDataBase('DB','MYSQL')");
				menuObj12.put("TOOL_TIP", "MYSQL");
				menuArray.add(menuObj12);
				JSONObject menuObj13 = new JSONObject();
				menuObj13.put("id", "MSSQL");
				menuObj13.put("PARENT_ID", "DATABASE");
				menuObj13.put("PARENT_MENU_ID", "DATABASE");
				menuObj13.put("MENU_ID", "MSSQL");
				menuObj13.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>MSSQL</span>");
				menuObj13.put("MAIN_DESCRIPTION", "MSSQL");
				menuObj13.put("icon", "images/MSSQL-Icon.png");
				menuObj13.put("value", "javascript:getTreeDataBase('DB','MSSQL')");
				menuObj13.put("TOOL_TIP", "MSSQL");
				menuArray.add(menuObj13);
				JSONObject menuObj14 = new JSONObject();
				menuObj14.put("id", "MSAccess");
				menuObj14.put("PARENT_ID", "DATABASE");
				menuObj14.put("PARENT_MENU_ID", "DATABASE");
				menuObj14.put("MENU_ID", "MSAccess");
				menuObj14.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>MSAccess</span>");
				menuObj14.put("MAIN_DESCRIPTION", "MSAccess");
				menuObj14.put("icon", "images/MSACCESS-Icon.svg");
				menuObj14.put("value", "javascript:getTreeDataBase('DB','MSAccess')");
				menuObj14.put("TOOL_TIP", "MSAccess");
				menuArray.add(menuObj14);
				JSONObject menuObj15 = new JSONObject();
				menuObj15.put("id", "SQLite");
				menuObj15.put("PARENT_ID", "DATABASE");
				menuObj15.put("PARENT_MENU_ID", "DATABASE");
				menuObj15.put("MENU_ID", "SQLite");
				menuObj15.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>SQLite</span>");
				menuObj15.put("MAIN_DESCRIPTION", "SQLite");
				menuObj15.put("icon", "images/SQLLITE-Icon.png");
				menuObj15.put("value", "javascript:getTreeDataBase('DB','SQLite')");
				menuObj15.put("TOOL_TIP", "SQLite");
				menuArray.add(menuObj15);
				JSONObject menuObj16 = new JSONObject();
				menuObj16.put("id", "PostgreSQL");
				menuObj16.put("PARENT_ID", "DATABASE");
				menuObj16.put("PARENT_MENU_ID", "DATABASE");
				menuObj16.put("MENU_ID", "PostgreSQL");
				menuObj16.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>PostgreSQL</span>");
				menuObj16.put("MAIN_DESCRIPTION", "PostgreSQL");
				menuObj16.put("icon", "images/DM_POSTGRESQL-Icon.png");
				menuObj16.put("value", "javascript:getTreeDataBase('DB','PostgreSQL')");
				menuObj16.put("TOOL_TIP", "PostgreSQL");
				menuArray.add(menuObj16);
				JSONObject menuObj17 = new JSONObject();
				menuObj17.put("id", "FACEBOOK");
				menuObj17.put("PARENT_ID", "ONL_SERVICES");
				menuObj17.put("PARENT_MENU_ID", "ONL_SERVICES");
				menuObj17.put("MENU_ID", "FACEBOOK");
				menuObj17.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>Facebook</span>");
				menuObj17.put("MAIN_DESCRIPTION", "Facebook");
				menuObj17.put("icon", "images/FB-Icon-01.png");
				menuObj17.put("value", "javascript:getTreeDataBase('Online_Services','Facebook')");
				menuObj17.put("TOOL_TIP", "Facebook");
				menuArray.add(menuObj17);
				JSONObject menuObj18 = new JSONObject();
				menuObj18.put("id", "Twitter");
				menuObj18.put("PARENT_ID", "ONL_SERVICES");
				menuObj18.put("PARENT_MENU_ID", "ONL_SERVICES");
				menuObj18.put("MENU_ID", "Twitter");
				menuObj18.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>Twitter</span>");
				menuObj18.put("MAIN_DESCRIPTION", "Twitter");
				menuObj18.put("icon", "images/TWITTER-Icon.svg");
				menuObj18.put("value", "javascript:getTreeDataBase('Online_Services','Twitter')");
				menuObj18.put("TOOL_TIP", "Twitter");
				menuArray.add(menuObj18);
				JSONObject menuObj19 = new JSONObject();
				menuObj19.put("id", "LinkedIn");
				menuObj19.put("PARENT_ID", "ONL_SERVICES");
				menuObj19.put("PARENT_MENU_ID", "ONL_SERVICES");
				menuObj19.put("MENU_ID", "LinkedIn");
				menuObj19.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>LinkedIn</span>");
				menuObj19.put("MAIN_DESCRIPTION", "LinkedIn");
				menuObj19.put("icon", "images/Linkedin-Icon-01.png");
				menuObj19.put("value", "javascript:getTreeDataBase('Online_Services','LinkedIn')");
				menuObj19.put("TOOL_TIP", "LinkedIn");
				menuArray.add(menuObj19);
				JSONObject menuObj21 = new JSONObject();
				menuObj21.put("id", "SAP");
				menuObj21.put("PARENT_ID", "ERP");
				menuObj21.put("PARENT_MENU_ID", "ERP");
				menuObj21.put("MENU_ID", "SAP");
				menuObj21.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>SAP</span>");
				menuObj21.put("MAIN_DESCRIPTION", "SAP");
				menuObj21.put("icon", "images/SAP_Ison-01.png");
				menuObj21.put("value", "javascript:getTreeDataBase('ERP','SAP')");
				menuObj21.put("TOOL_TIP", "SAP");
				menuArray.add(menuObj21);
				JSONObject menuObj22 = new JSONObject();
				menuObj22.put("id", "ORACLE_ERP");
				menuObj22.put("PARENT_ID", "ERP");
				menuObj22.put("PARENT_MENU_ID", "ERP");
				menuObj22.put("MENU_ID", "ORACLE_ERP");
				menuObj22.put("MENU_DESCRIPTION", "<span class='visionMenuTreeLabel'>ORACLE ERP</span>");
				menuObj22.put("MAIN_DESCRIPTION", "ORACLE ERP");
				menuObj22.put("icon", "images/DM_ORA_ERP-Icon-01.png");
				menuObj22.put("value", "javascript:getTreeDataBase('ERP','Oracle_ERP')");
				menuObj22.put("TOOL_TIP", "ORACLE ERP");
				menuArray.add(menuObj22);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return menuArray;

	}

	public JSONObject importTreeDMFileXlsx(HttpServletRequest request, HttpServletResponse response,
			JSONObject jsonData, String selectedFiletype) {

		FileInputStream inputStream = null;
		FileOutputStream outs = null;
		JSONObject importResult = new JSONObject();
		try {
			if (true) {
				// fis = new FileInputStream(new File(filepath));
				String originalFileName = request.getParameter("fileName");
				String userName = (String) request.getSession(false).getAttribute("ssUsername");
				String filePath = fileStoreHomedirectory + "TreeDMImport/" + userName;
//				String filePath = "C:/Files/TreeDMImport" + File.separator + userName;

				String mainFileName = "SPIRUploadSheet" + System.currentTimeMillis() + "." + selectedFiletype;
				String fileName = filePath + File.separator + mainFileName;

				String headersObjStr = request.getParameter("headersObj");
				JSONObject headersObj = (JSONObject) JSONValue.parse(headersObjStr);

				String sheetsStr = request.getParameter("sheets");
				JSONArray sheetsArray = (JSONArray) JSONValue.parse(sheetsStr);

				File outputFile = new File(filePath);
				if (outputFile.exists()) {
					System.out.println("folder is deleted");
					outputFile.delete();
				}
				if (!outputFile.exists()) {
					System.out.println("folder is created");
					outputFile.mkdirs();
				}else {
					System.out.println("folder is not created");
				}

				XSSFWorkbook outputWb = new XSSFWorkbook();
//                Workbook outputWb = (XSSFWorkbook) WorkbookFactory.create(new File(fileName));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				CellStyle dateCellStyle = outputWb.createCellStyle();
				CellStyle timeCellStyle = outputWb.createCellStyle();
				CreationHelper createHelper = outputWb.getCreationHelper();
				dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
				timeCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("h:mm:ss"));

				for (Object sheet : sheetsArray) {
					XSSFSheet outputSheet = outputWb.createSheet((String) sheet);

					JSONArray sheetData = (JSONArray) jsonData.get(sheet);
					JSONArray sheetHeaders = (JSONArray) headersObj.get(sheet);
					XSSFRow outPutHeader = outputSheet.createRow(0);

					for (int cellIndex = 0; cellIndex < sheetHeaders.size(); cellIndex++) {

						WritableFont cellFont = new WritableFont(WritableFont.TIMES, 16);

						WritableCellFormat cellFormat = new WritableCellFormat(cellFont);
						cellFormat.setBackground(Colour.ORANGE);
						XSSFCellStyle cellStyle = outputWb.createCellStyle();
						cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
						cellStyle.setWrapText(true);

						String cellValue = (String) sheetHeaders.get(cellIndex);

						XSSFCell hssfCell = outPutHeader.createCell(cellIndex);
						hssfCell.setCellStyle(cellStyle);

						hssfCell.setCellValue(cellValue);

					}

					for (int i = 0; i < sheetData.size(); i++) {

						XSSFRow outPutRow = outputSheet.createRow(i + 1);

						JSONObject rowData = (JSONObject) sheetData.get(i);
						if (rowData != null) {

							for (int cellIndex = 0; cellIndex < sheetHeaders.size(); cellIndex++) {
								String header = (String) sheetHeaders.get(cellIndex);
								Object cellValue = rowData.get(header);
								XSSFCell outputCell = null;
								try {
//                            System.out.println(i+ " cellIndex::::" + cellIndex);
									outputCell = outPutRow.createCell(cellIndex);
									if (cellValue != null) {

										if (cellValue instanceof String) {
											if (isValidDate((String) cellValue)) {
												Date date = sdf.parse((String) cellValue);
												outputCell.setCellValue(date);
												if (((String) cellValue).contains("1899-12-31T")) {
													String timeStr = ((String) cellValue).substring(11, 19);
													Double timeDouble = DateUtil.convertTime(timeStr);
													outputCell.setCellValue(timeDouble);
													outputCell.setCellStyle(timeCellStyle);
												} else {
													outputCell.setCellStyle(dateCellStyle);
												}

											} else {
												outputCell.setCellValue((String) cellValue);
											}

//                                            outputCell.setCellType(CellType._NONE);
										} else if (cellValue instanceof Number) {
											outputCell.setCellValue(Double.valueOf(String.valueOf(cellValue)));
										} else if (cellValue instanceof Boolean) {
											outputCell.setCellValue((Boolean) cellValue);
										} else {
											outputCell.setCellValue(String.valueOf(cellValue));
										}

									} else {
										outputCell.setCellValue("");
									}

								} catch (Exception e) {
									outputCell.setCellValue("");
									continue;
								}

							}
						}

					}
				}
				outs = new FileOutputStream(fileName);
				outputWb.write(outs);
				outs.close();
				try {
//                    dashBoardsDAO.saveUserFiles(request, originalFileName, mainFileName, filePath, selectedFiletype);
				} catch (Exception e) {
				}
				String gridId = "divGrid-" + mainFileName.replace("." + selectedFiletype, "");
				gridId = gridId.replace(".csv", "");

				importResult = getFileObjectMetaData(request, response, fileName, gridId, selectedFiletype,
						mainFileName);
				importResult.put("fileExist", dashBoardsDAO.checkExistMergeTableName(request));     

			}
			// return result1;
			if (inputStream != null) {
				inputStream.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return importResult;
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

	public JSONObject getFileObjectMetaData(HttpServletRequest request, HttpServletResponse response, String filePath,
			String gridId, String fileType, String fileFolderPath) {
		JSONObject fileMetaObj = new JSONObject();
		try {
			JSONArray dataFieldsArray = new JSONArray();
			JSONArray columnsArray = new JSONArray();    

			List<String> headers = getHeadersOfImportedFile(request, filePath);
			Map<String,String> headerDataTypes =dashBoardsDAO.getDataTypesOFHeader(request);
			if (!(headers != null && !headers.isEmpty())) {
				String fileHeadersStr = request.getParameter("fileHeaders");
				if (fileHeadersStr != null && !"".equalsIgnoreCase(fileHeadersStr)
						&& !"[]".equalsIgnoreCase(fileHeadersStr)) {
					JSONObject fileHeaders = (JSONObject) JSONValue.parse(fileHeadersStr);
					headers = new ArrayList(fileHeaders.values()); 
				}

			}
			String gridPersonalizeStr = "";    
			if (headers != null && !headers.isEmpty()) {
				List<String> columnList = new ArrayList();
				if(!(headers.contains("AUDIT_ID") || headers.contains("Audit_Id") || headers.contains("audit_id")))
				{
					headers.add("AUDIT_ID");
				}
				for (int i = 0; i < headers.size(); i++) {
					String header = headers.get(i);
					if (header != null && !"".equalsIgnoreCase(header) && !"".equalsIgnoreCase(header)) {
						header = header.toUpperCase();
						String colLabel = header.toLowerCase().replace("_", " ");
						String headerText = Stream.of(colLabel.trim().split("\\s")).filter(word -> word.length() > 0)
								.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
								.collect(Collectors.joining(" "));
						JSONObject dataFieldsObj = new JSONObject();
						columnList.add(header.replaceAll("\\s", "_"));
						gridPersonalizeStr += "<tr>" + "<td>" + header + "</td>" + "<td>"
								+ "<input type='checkbox' data-gridid='" + gridId + "' checked id='" + gridId + "_"
								+ header.replaceAll("\\s", "_") + "_DISPLAY' data-type='display' " + " data-colname='"
								+ header.replaceAll("\\s", "_") + "' onchange=\"updateETLPersonalize(id)\"" + "</td>"
								+ "<td>" + "<input type='checkbox' id='" + gridId + "_" + header.replaceAll("\\s", "_")
								+ "_FREEZE' data-gridid='" + gridId + "' data-type='pinned' " + " data-colname='"
								+ header.replaceAll("\\s", "_") + "' onchange=\"updateETLPersonalize(id)\"" + "</td>"
								+ "</tr>";
						dataFieldsObj.put("name", header.replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9_]", "_"));
						dataFieldsObj.put("type", headerDataTypes.get(header.replaceAll("[^a-zA-Z0-9_]", "_")));

						dataFieldsArray.add(dataFieldsObj);

						JSONObject columnsObject = new JSONObject();

						columnsObject.put("text", headerText);
						columnsObject.put("datafield", header.replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9_]", "_"));
						columnsObject.put("width", 120);
						columnsArray.add(columnsObject);

					}
				}
				gridPersonalizeStr = "<div class=\"personaliseoption visionSearchPersonaliseoption\" style=\"margin-top:5px;\">"
						+ "<div onclick=slideSettingsETL('" + gridId + "') class=\"layoutoptions ui-accordion\">"
						+ "<h3 class=\"ui-accordion-header1\"><span class=\"ui-accordion-header-icon ui-icon1 "
						+ " ui-icon-triangle-1-e ui-icon-triangle-1-s\" id=\"" + gridId + "_personalizeid\"></span>"
						+ "<img alt=\"\" class=\"navIcon gear\" src=\"images/f_spacer.gif\">Personalize</h3>"
						+ "</div><div id=\"" + gridId
						+ "_settings_panel\" class=\"VisionETLSettings_panel\" style=\"display: none;\">"
						+ "<div class=\"personalize\" id=\"" + gridId
						+ "_personalize_fields\"> <div class=\"pers_content\">"
						+ " <div id=\"tg-wrap4\" class=\"VisionETL-tg-wrap visionSearchPersonalise\"> "
						+ "<div class=\"visionPersonaliseSticky\"> <div class=\"sticky-wrap\"> "
						+ " <div class=\"sticky-wrap\">"
						+ "<table class=\"personalize_tbl sticky-enabled\" id=\"pers_criteria\" style=\"margin: 0px; width: 100%;\"> "
						+ "  <thead> <tr style=\"\"><th>Parameter</th><th>Display</th><th>Freeze</th>	   </tr>   </thead>  "
						+ " <tbody>" + gridPersonalizeStr + "</tbody>"
						+ "</table></div></div></div></div></div></div></div></div>";

				// ravi multiple excelsheets sheets
				if (fileType != null && ("XLS".equalsIgnoreCase(fileType.toUpperCase())
						|| "XLSX".equalsIgnoreCase(fileType.toUpperCase()))) {
					Workbook workBook = WorkbookFactory.create(new File(filePath));
					int sheetCount = workBook.getNumberOfSheets();
					if (sheetCount > 1) {
						String navDiv = "<div id='navBar_" + gridId + "'><ul style='width: fit-content;'>";

						for (int i = 0; i < sheetCount; i++) {
							navDiv += "<li width='70px' >" + workBook.getSheetName(i) + "</li>";
						}
						navDiv += "</ul></div>";
						fileMetaObj.put("navigationDiv", navDiv);
					}
				}
				fileMetaObj.put("gridPersonalizeStr", gridPersonalizeStr);
				fileMetaObj.put("dataFieldsArray", dataFieldsArray);
				fileMetaObj.put("columnsArray", columnsArray);
				fileMetaObj.put("columnList", columnList);
				fileMetaObj.put("gridId", gridId);
				fileMetaObj.put("filePath", fileFolderPath);    
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileMetaObj;
	}



	public List getHeadersOfImportedFile(HttpServletRequest request, String filePath) {
		List<String> headers = null;
		try {
			if (filePath != null && !"".equalsIgnoreCase(filePath) && !"null".equalsIgnoreCase(filePath)) {
				String fileExt = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());

				if (fileExt != null && !"".equalsIgnoreCase(fileExt)) {
					if ("txt".equalsIgnoreCase(fileExt) || "csv".equalsIgnoreCase(fileExt)) {

						CsvParserSettings settings = new CsvParserSettings();
						settings.detectFormatAutomatically();

						CsvParser parser = new CsvParser(settings);
						List<String[]> rows = parser.parseAll(new File(filePath));

						// if you want to see what it detected
//                        CsvFormatDetector formatdetect =  new CsvFormatDetector();
						CsvFormat format = parser.getDetectedFormat();
						char columnSeparator = format.getDelimiter();

						String fileType = request.getParameter("fileType");
//                        char columnSeparator = '\t';
//                        char columnSeparator = ',';
						if (!(fileType != null && !"".equalsIgnoreCase(fileType)
								&& !"null".equalsIgnoreCase(fileType))) {
							fileType = (String) request.getAttribute("fileType");
						}
						if (".json".equalsIgnoreCase(fileType)) {
							columnSeparator = ',';
						}

						CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filePath), "UTF8"),
								columnSeparator);

						String[] nextLine;
						while ((nextLine = reader.readNext()) != null) {
							if (nextLine.length != 0 && nextLine[0].contains("" + columnSeparator)) {
								headers = new ArrayList<>(Arrays.asList(nextLine[0].split("" + columnSeparator)));
							} else {
								headers = new ArrayList<>(Arrays.asList(nextLine));
							}

							break;
						}
					} else if ("xls".equalsIgnoreCase(fileExt) || "xlsx".equalsIgnoreCase(fileExt)) {
						headers = new ArrayList<>();
						Workbook workBook = null;
						Sheet sheet = null;

						// PKH sheet Header
						String sheetNum = request.getParameter("sheetNo");
						int sheetNo = (sheetNum != null && !"".equalsIgnoreCase(sheetNum))
								? (Integer.parseInt(sheetNum))
								: 0;
						// PKH sheet Header

//                        if (fileExt != null && "xls".equalsIgnoreCase(fileExt)) { // commented by PKH
//                            workBook = WorkbookFactory.create(new File(filePath));
//                            sheet = (HSSFSheet) workBook.getSheetAt(sheetNo);
//                        } else {
						System.out.println("Before::::" + new Date());
//                fis = new FileInputStream(new File(filepath));              
//                XSSFWorkbook xssfWb = (XSSFWorkbook) new XSSFWorkbook(fis);
						workBook = WorkbookFactory.create(new File(filePath));
						System.out.println("After::fileInputStream::" + new Date());
						sheet = (XSSFSheet) workBook.getSheetAt(sheetNo);
//                sheet = (XSSFSheet) xssfWb.getSheetAt(0);
//                        }
						if (sheet != null) {
							Row row = sheet.getRow(0);
							if (row != null) {
								for (int j = 0; j < row.getLastCellNum(); j++) {
									// System.out.println("Cell Num:::" + j + ":::Start Date And Time :::" + new
									// Date());

									try {
										Cell cell = row.getCell(j);

										if (cell != null) {
											switch (cell.getCellType()) {
											case Cell.CELL_TYPE_STRING:
												headers.add(cell.getStringCellValue());
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
													headers.add(cellDateString);
//                                            
												} else {
													String cellvalStr = NumberToTextConverter
															.toText(cell.getNumericCellValue());
													headers.add(cellvalStr);
												}
												break;
											case Cell.CELL_TYPE_BLANK:
												headers.add("");
												break;
											}

										} else {
											headers.add("");
//                            testMap.put(stmt, "");
										}
									} catch (Exception e) {
										e.printStackTrace();
										headers.add("");
										continue;
									}

								} // end of row cell loop
							}
						}

					} else if ("xml".equalsIgnoreCase(fileExt)) {
						headers = new ArrayList<>();
						DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
						DocumentBuilder builder = factory.newDocumentBuilder();
						Document document = builder.parse(new File(filePath));
						document.getDocumentElement().normalize();
						Element root = document.getDocumentElement();
						if (root.hasChildNodes() && root.getChildNodes().getLength() > 1) {
							String evaluateTagName = "/" + root.getTagName();
							NodeList rootList = root.getChildNodes();
							if (!"#Text".equalsIgnoreCase(rootList.item(0).getNodeName())) {
								evaluateTagName += "/" + rootList.item(0).getNodeName();
							} else {
								evaluateTagName += "/" + rootList.item(1).getNodeName();
							}

							System.out.println("evaluateTagName:::" + evaluateTagName);
							XPath xpath = XPathFactory.newInstance().newXPath();
							NodeList dataNodeList = (NodeList) xpath.evaluate(evaluateTagName,
									// NodeList nList = (NodeList) xpath.evaluate("/PiLog_Data_Export/Item",
									document, XPathConstants.NODESET);
							if (dataNodeList != null && dataNodeList.getLength() != 0) {
								int rowCount = dataNodeList.getLength();
								Node node = dataNodeList.item(0);
								if (node.getNodeType() == Node.ELEMENT_NODE) {
									NodeList childNodeList = node.getChildNodes();
									for (int i = 0; i < childNodeList.getLength(); i++) {// Columns

										Node childNode = childNodeList.item(i);
										if (childNode != null && !"#Text".equalsIgnoreCase(childNode.getNodeName())) {
											headers.add(childNode.getNodeName());
											System.err.println(
													childNode.getNodeName() + "---> " + childNode.getTextContent());
										}

									} // end of columns loop

								}

							}

						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return headers;
	}

	public List getFileObjectData(HttpServletRequest request, HttpServletResponse response) {
		List dataList = new ArrayList();
		try {
			String filePath = request.getParameter("filePath");
			if (filePath != null && !"".equalsIgnoreCase(filePath)) {
				String targetFile = request.getParameter("targetFile"); // ravi etl new issues
				if ("Y".equalsIgnoreCase(targetFile)) {
					filePath = "C:/ETL_EXPORT_" + File.separator + request.getSession(false).getAttribute("ssUsername")
							+ File.separator + filePath;
				} else {
					filePath = fileStoreHomedirectory + "TreeDMImport/"
							+ request.getSession(false).getAttribute("ssUsername") + File.separator + filePath;

				}
				// C:/Files/TreeDMImport/SAN_MGR_MM
//                filePath = "C:/Files/TreeDMImport" + File.separator + request.getSession(false).getAttribute("ssUsername") + File.separator + filePath;
			}
			String fileName = request.getParameter("fileName");
			String fileType = request.getParameter("fileType");
			if (fileType != null && !"".equalsIgnoreCase(fileType) && !fileType.startsWith(".")) {
				fileType = "." + fileType;
			}
			String columnsArray = request.getParameter("columnsArray");
			List<String> columnList = new ArrayList<>();
			if (columnsArray != null && !"".equalsIgnoreCase(columnsArray) && !"null".equalsIgnoreCase(columnsArray)) {
				columnList = (List<String>) JSONValue.parse(columnsArray);
			}
			if (".xls".equalsIgnoreCase(fileType) || ".xlsx".equalsIgnoreCase(fileType)) {
				dataList = readExcel(request, response, filePath, columnList);
			} else if (".CSV".equalsIgnoreCase(fileType) || ".TXT".equalsIgnoreCase(fileType)
					|| ".JSON".equalsIgnoreCase(fileType)) {
				dataList = readCSV(request, response, filePath, columnList);
			} else if (".xml".equalsIgnoreCase(fileType)) {
				dataList = readXML(request, response, filePath, columnList);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataList;
	}

	public List readExcel(HttpServletRequest request, HttpServletResponse response, String filepath,
			List<String> columnList) {

		FileInputStream fis = null;

		System.out.println("Start Date And Time :::" + new Date());
		List dataList = new ArrayList();
		int rowVal = 1;
		try {
			if (true) {
				// fis = new FileInputStream(new File(filepath));

				Workbook workBook = null;
				Sheet sheet = null;
				String sheetNum = request.getParameter("sheetNo");// ravi multiple excel sheet

				int sheetNo = (sheetNum != null && !"".equalsIgnoreCase(sheetNum)) ? (Integer.parseInt(sheetNum)) : 0;// ravi
																														// multiple
																														// excel
																														// sheet

				String fileExtension = filepath.substring(filepath.lastIndexOf(".") + 1, filepath.length());
				System.out.println("fileExtension:::" + fileExtension);
//                if (fileExtension != null && "xls".equalsIgnoreCase(fileExtension)) { //commented by PKH
//                    workBook = WorkbookFactory.create(new File(filepath));
//                    sheet = (HSSFSheet) workBook.getSheetAt(sheetNo);
//                } else {
				System.out.println("Before::::" + new Date());
				workBook = WorkbookFactory.create(new File(filepath));
				System.out.println("After::fileInputStream::" + new Date());
				sheet = (XSSFSheet) workBook.getSheetAt(sheetNo);
//                }
				int lastRowNo = sheet.getLastRowNum();
				System.out.println("lastRowNo::::" + lastRowNo);
				int firstRowNo = sheet.getFirstRowNum();
				System.out.println("firstRowNo::::" + firstRowNo);
				int rowCount = lastRowNo - firstRowNo;
				System.out.println("rowCount:::::" + rowCount);

				int stmt = 1;
				String strToDateCol = "";
				String pagenum = request.getParameter("pagenum");
				String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize") : "10";
				String recordendindex = request.getParameter("recordendindex");
				String recordstartindex = (request.getParameter("recordstartindex"));
				Integer filterscount = 0;
				if (request.getParameter("filterscount") != null) {
					filterscount = new Integer(request.getParameter("filterscount"));
				}
				String sortdatafield = request.getParameter("sortdatafield");
				System.out.println("sortdatafield::::" + sortdatafield);
				String sortorder = request.getParameter("sortorder");
				if (!(sortdatafield != null && !"".equalsIgnoreCase(sortdatafield))) {
					sortdatafield = (String) request.getAttribute("sortdatafield");
				}
				if (!(sortorder != null && !"".equalsIgnoreCase(sortorder))) {
					sortorder = (String) request.getAttribute("sortorder");
				}

				rowVal = 1;
				if (recordstartindex != null && !"".equalsIgnoreCase(recordstartindex)
						&& !"null".equalsIgnoreCase(recordstartindex) && !"0".equalsIgnoreCase(recordstartindex)) {
					rowVal = Integer.parseInt(recordstartindex);
				}
				int endIndex = rowCount + 1;
				if (recordendindex != null && !"".equalsIgnoreCase(recordendindex)
						&& !"null".equalsIgnoreCase(recordendindex) && Integer.parseInt(recordendindex) <= rowCount) {
					endIndex = Integer.parseInt(recordendindex) + 1;
				}
				System.out.println("endIndex:::::" + endIndex);
				for (int i = rowVal; i < endIndex; i++) {
					Row row = sheet.getRow(i);
					stmt = 1;
					JSONObject dataObject = new JSONObject();
					dataObject.put("totalrecords", rowCount);
					for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {

						try {
//                            System.out.println("cellIndex::::" + cellIndex);
							Cell cell = row.getCell(cellIndex);
							if (cell != null) {
								switch (cell.getCellType()) {
								case Cell.CELL_TYPE_STRING:
									String cellValue = cell.getStringCellValue();
									if (cellValue != null && !"".equalsIgnoreCase(cellValue)
											&& !"null".equalsIgnoreCase(cellValue)) {
										dataObject.put(columnList.get(cellIndex), cellValue);
									} else {
										dataObject.put(columnList.get(cellIndex), "");
									}

									break;
								case Cell.CELL_TYPE_BOOLEAN:
//                                rowObj.put(header, hSSFCell.getBooleanCellValue());
									break;
								case Cell.CELL_TYPE_NUMERIC:

									if (HSSFDateUtil.isCellDateFormatted(cell)) {
										if (strToDateCol != null && !"".equalsIgnoreCase(strToDateCol)
												&& !"null".equalsIgnoreCase(strToDateCol)
												&& strToDateCol.contains(String.valueOf(stmt))) {
											DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
											Date convertedDate = (Date) formatter.parse(cell.toString());
											dataObject.put(columnList.get(cellIndex), cell.toString());

//                                            testMap.put(stmt, sqlDat);
										} else {
											String cellDateString = "";
											Date cellDate = cell.getDateCellValue();
											if ((cellDate.getYear() + 1900) == 1899 && (cellDate.getMonth() + 1) == 12
													&& (cellDate.getDate()) == 31) {
												cellDateString = (cellDate.getHours()) + ":" + (cellDate.getMinutes())
														+ ":" + (cellDate.getSeconds());
//                                                    System.out.println("cellDateString :: "+cellDateString);
											} else {
												cellDateString = (cellDate.getYear() + 1900) + "-"
														+ (cellDate.getMonth() + 1) + "-" + (cellDate.getDate());
											}

//                                                String cellDateString = (cellDate.getYear() + 1900) + "-" + (cellDate.getMonth() + 1) + "-" + (cellDate.getDate());
											dataObject.put(columnList.get(cellIndex), cellDateString);
										}

									} else {
										String cellvalStr = NumberToTextConverter.toText(cell.getNumericCellValue());
										dataObject.put(columnList.get(cellIndex), cellvalStr);
									}
									break;
								case Cell.CELL_TYPE_BLANK:
									dataObject.put(columnList.get(cellIndex), "");
									break;
								}

							} else {
								dataObject.put(columnList.get(cellIndex), "");
							}
						} catch (Exception e) {
							dataObject.put(columnList.get(cellIndex), "");
							continue;
						}

					} // end of row cell loop
					dataList.add(dataObject);
				} // row end

				// return result1;
				if (fis != null) {
					fis.close();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

		return dataList;
	}

	public List readCSV(HttpServletRequest request, HttpServletResponse response, String filepath,
			List<String> columnList) {
		FileInputStream fis = null;
		System.out.println("Start Date And Time :::" + new Date());
		List dataList = new ArrayList();
		int rowVal = 1;
		try {
			int rowCount = 0;
			// fis = new FileInputStream(new File(filepath));
			String fileType = request.getParameter("fileType");
			String fileExtension = filepath.substring(filepath.lastIndexOf(".") + 1, filepath.length());
			System.out.println("fileExtension:::" + fileExtension);

			int stmt = 1;
			String strToDateCol = "";
//            char colSepartor = '\t';

			CsvParserSettings settings = new CsvParserSettings();
			settings.detectFormatAutomatically();

			CsvParser parser = new CsvParser(settings);
			List<String[]> rows = parser.parseAll(new File(filepath));

			// if you want to see what it detected
//                        CsvFormatDetector formatdetect =  new CsvFormatDetector();
			CsvFormat format = parser.getDetectedFormat();
			char colSepartor = format.getDelimiter();

//            char colSepartor = ',';
			if (".JSON".equalsIgnoreCase(fileType) || "json".equalsIgnoreCase(fileType)) {
				colSepartor = ',';
			}
			// need to write logic for extraction from File
			CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filepath), "UTF8"), colSepartor);
			LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(filepath));
			lineNumberReader.skip(Long.MAX_VALUE);
			long totalRecords = lineNumberReader.getLineNumber();
			if (totalRecords != 0) {
				totalRecords = totalRecords - 1;
			}
			System.out.println("totalRecords:::" + totalRecords);
//             CSVReader  reader = new CSVReader(new FileReader(filepath),'\t');
			String pagenum = request.getParameter("pagenum");
			String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize") : "10";
			String recordendindex = request.getParameter("recordendindex");
			String recordstartindex = (request.getParameter("recordstartindex"));
			rowVal = 1;

			if (recordstartindex != null && !"".equalsIgnoreCase(recordstartindex)
					&& !"null".equalsIgnoreCase(recordstartindex) && !"0".equalsIgnoreCase(recordstartindex)) {
				rowVal = Integer.parseInt(recordstartindex);
			}
//            int endIndex = (int)totalRecords + 1;
//            if (recordendindex != null
//                    && !"".equalsIgnoreCase(recordendindex)
//                    && !"null".equalsIgnoreCase(recordendindex)
//                    && Integer.parseInt(recordendindex) <= rowCount) {
//                endIndex = Integer.parseInt(recordendindex);
//            }
			int skipLines = 0;
			if (pagenum != null && !"".equalsIgnoreCase(pagenum) && !"null".equalsIgnoreCase(pagenum)
					&& !"0".equalsIgnoreCase(pagenum) && pagesize != null && !"".equalsIgnoreCase(pagesize)
					&& !"null".equalsIgnoreCase(pagesize)) {
				skipLines = Integer.parseInt(pagenum) * Integer.parseInt(pagesize);
			}
			if (skipLines == 0) {
				String[] headers = reader.readNext();
			}
			reader.skip(skipLines);

			String[] nextLine;
			int rowsCount = 1;
			while ((nextLine = reader.readNext()) != null) {// no of rows
				if (Integer.parseInt(pagesize) >= rowsCount) {
					rowsCount++;

					JSONObject dataObject = new JSONObject();
					dataObject.put("totalrecords", totalRecords);
					for (int j = 0; j < columnList.size(); j++) {
						try {
							int cellIndex = j;
							if (cellIndex <= (nextLine.length - 1)) {
								String token = nextLine[cellIndex];
								if (token != null && !"".equalsIgnoreCase(token)) {
									try {
										dataObject.put(columnList.get(j), token);
									} catch (Exception e) {
										dataObject.put(columnList.get(j), "");
										continue;
									}
								} else {
									dataObject.put(columnList.get(j), "");
								}
							} else {
								dataObject.put(columnList.get(j), "");
							}
						} catch (Exception e) {
							dataObject.put(columnList.get(j), "");
							continue;
						}

					}

					dataList.add(dataObject);
				} else {
					break;
				}

			}

			if (fis != null) {
				fis.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}
//

	public List readXML(HttpServletRequest request, HttpServletResponse response, String filepath,
			List<String> columnList) {
		FileInputStream fis = null;
		List dataList = new ArrayList();
		try {
			int rowCount = 0;
			String fileExtension = filepath.substring(filepath.lastIndexOf(".") + 1, filepath.length());
			System.out.println("fileExtension:::" + fileExtension);

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new FileInputStream(filepath), "UTF-8");
			document.getDocumentElement().normalize();
			Element root = document.getDocumentElement();

			if (root.hasChildNodes() && root.getChildNodes().getLength() > 1) {
				// nested childs
				String evaluateTagName = "/" + root.getTagName();
				NodeList rootList = root.getChildNodes();
				if (!"#Text".equalsIgnoreCase(rootList.item(0).getNodeName())) {
					evaluateTagName += "/" + rootList.item(0).getNodeName();
				} else {
					evaluateTagName += "/" + rootList.item(1).getNodeName();
				}

				System.out.println("evaluateTagName:::" + evaluateTagName);
				XPath xpath = XPathFactory.newInstance().newXPath();
				NodeList dataNodeList = (NodeList) xpath.evaluate(evaluateTagName,
						// NodeList nList = (NodeList) xpath.evaluate("/PiLog_Data_Export/Item",
						document, XPathConstants.NODESET);

				if (dataNodeList != null && dataNodeList.getLength() != 0) {
					rowCount = dataNodeList.getLength();
					String pagenum = request.getParameter("pagenum");
					String pagesize = request.getParameter("pagesize") != null ? request.getParameter("pagesize")
							: "10";
					String recordendindex = request.getParameter("recordendindex");
					String recordstartindex = (request.getParameter("recordstartindex"));
					// need to write logic for extraction from File
					int startIndex = 0;
					if (recordstartindex != null && !"".equalsIgnoreCase(recordstartindex)
							&& !"null".equalsIgnoreCase(recordstartindex) && !"0".equalsIgnoreCase(recordstartindex)) {
						startIndex = Integer.parseInt(recordstartindex);
					}
					int endIndex = rowCount;
					if (recordendindex != null && !"".equalsIgnoreCase(recordendindex)
							&& !"null".equalsIgnoreCase(recordendindex)
							&& Integer.parseInt(recordendindex) <= rowCount) {
						endIndex = Integer.parseInt(recordendindex);
					}
					int skipLines = 0;
					if (pagenum != null && !"".equalsIgnoreCase(pagenum) && !"null".equalsIgnoreCase(pagenum)
							&& !"1".equalsIgnoreCase(pagenum) && pagesize != null && !"".equalsIgnoreCase(pagesize)
							&& !"null".equalsIgnoreCase(pagesize)) {
						skipLines = Integer.parseInt(pagenum) * Integer.parseInt(pagesize);
					}

					for (int temp = startIndex; temp < endIndex; temp++) {// Rows
						Node node = dataNodeList.item(temp);
						JSONObject dataObject = new JSONObject();
						dataObject.put("totalrecords", rowCount);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							NodeList childNodeList = node.getChildNodes();
							for (int j = 0; j < columnList.size(); j++) {
								try {
									int childNodeIndex = j;
									int nodeListLength = childNodeList.getLength();
									if (childNodeIndex <= (childNodeList.getLength() - 1)) {
										Node childNode = childNodeList.item(childNodeIndex);
										if (childNode != null) {
											if (childNode != null && childNode.getNodeType() == Node.ELEMENT_NODE) {
												try {
													if (childNode.getTextContent() != null
															&& !"".equalsIgnoreCase(childNode.getTextContent())
															&& !"null".equalsIgnoreCase(childNode.getTextContent())) {
														dataObject.put(columnList.get(j), childNode.getTextContent());

													} else {
														dataObject.put(columnList.get(j), "");
													}

												} catch (Exception e) {
													dataObject.put(columnList.get(j), "");
													continue;
												}
												// Need to set the Data

											}
										} else {
											dataObject.put(columnList.get(j), "");
										}
									} else {
										dataObject.put(columnList.get(j), "");
									}
								} catch (Exception e) {
									dataObject.put(columnList.get(j), "");
									continue;
								}

							} // column list loop

						}
						dataList.add(dataObject);
					} // end of rows loop

				}
			} else {
				System.err.println("*** Root Element Not Found ****");
			}

			if (fis != null) {
				fis.close();
			}
		} catch (Exception e) {
			e.printStackTrace();

		}

		return dataList;
	}

	public String getLoadTableColumns(HttpServletRequest request) {
		return dashBoardsDAO.getLoadTableColumns(request);
	}

	public JSONObject fetchChartData(HttpServletRequest request) { 
		JSONObject chartObj = new JSONObject();
		try {
			String chartType = request.getParameter("chartType");
			if (chartType != null && !"".equalsIgnoreCase(chartType) && "heatMap".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchHeatMapEChartData(request);
			} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "sunburst".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchSunbrstEChartData(request);
			} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "geochart".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchGeoChartData(request);
			} else if (chartType != null && !"".equalsIgnoreCase(chartType)
					&& "BarAndLine".equalsIgnoreCase(chartType)) { 
				chartObj = dashBoardsDAO.fetchBarwithLineEChartData(request);
			} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "treemap".equalsIgnoreCase(chartType)) { 
				chartObj = dashBoardsDAO.fetchTreeMapEChartData(request);
			} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "boxplot".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchBoxPlotChartData(request);
			} else if (chartType != null && !"".equalsIgnoreCase(chartType) && "sankey".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchSankeyChartData(request);
			} else if (chartType != null && !"".equalsIgnoreCase(chartType)
					&& "horizontal_bar".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchHorizontalBarChartData(request);    
			} else if (chartType != null && !"".equalsIgnoreCase(chartType)
					&& "ganttChart".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchGanttChartData(request);    
			} else {
				chartObj = dashBoardsDAO.fetchChartData(request);        
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return chartObj;
	}

	public JSONObject fetchFiltersValues(HttpServletRequest request) {
		return dashBoardsDAO.fetchFiltersValues(request); 
	}

	public JSONObject fetchSlicerValues(HttpServletRequest request) {
		return dashBoardsDAO.fetchSlicerValues(request);             
	}

	public JSONObject fetchSlicerButtonValues(HttpServletRequest request) {
		return dashBoardsDAO.fetchSlicerButtonValues(request);
	}

	public JSONObject fetchSlicerListValues(HttpServletRequest request) {
		return dashBoardsDAO.fetchSlicerListValues(request); 
	}

	public JSONObject fetchSlicerDropdownValues(HttpServletRequest request) {
		return dashBoardsDAO.fetchSlicerDropdownValues(request);
	}

	public JSONObject getChartFilters(HttpServletRequest request) {  
		JSONObject jsonChartFilter = new JSONObject();
		String startUltag = "<ul class='conigProperties'>";
		String endUltag = "</ul>";
		String pieChart = startUltag;
		pieChart += getGeneralFilters("PIE");
		pieChart += pieDonutGeneralFilters("PIE");
		pieChart += getLegendFilters("PIE");
		pieChart += getChartColors("PIE");
		pieChart += getChartHover("PIE", "layout");
		pieChart += endUltag;

		String gaugeChart = startUltag;
		gaugeChart += getIndicatorFilters("INDICATOR");
		gaugeChart += endUltag;

		String heatMap = startUltag;
		heatMap += getTitleFilters("HEATMAP");
		heatMap += endUltag;

		String donutChart = startUltag;
		donutChart += getGeneralFilters("DONUT");
		donutChart += pieDonutGeneralFilters("DONUT");
		donutChart += getLegendFilters("DONUT");
		donutChart += getChartColors("DONUT");
		donutChart += getChartHover("DONUT", "layout");
		// need to fix
//		donutChart += "<li id=\"hole-filter\" data-column-name=\"DONUTHOLE\">" + "<div class=\"main-container\">"
//				+ "<div class=\"filter-container\">"
//				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>"
//				+ "<p>Hole Radius</p>" + "</div>" + getToggleButton("DONUT", "") + "</div>"
//				+ "<ul class=\"sub-filters\" id=\"DONUTHOLE\" style=\"display: none;\">"
//				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"DONUTHOLERADIUS\">"
//				+ "<p>Inner Radius</p>" + "<input type=\"text\"id=\"DONUTHOLERADIUS\" data-opt-name=\"hole\"/>"
//				+ "<input type=\"range\" data-opt-name=\"hole\"/>" + "</li>" + "</ul>" + "</li>";
		donutChart += endUltag;

		String barChart = startUltag;
		barChart += getGeneralFilters("BAR");
		barChart += getLabelAndHoverDataFilters("BAR");
		barChart += "<li class=\"general-filters active-filter\" data-column-name=\"BARMODE\" data-key-type=\"layout\">"
				+ "<div class=\"sub-filterItems\">" + "<p>Bar Mode</p>"
				+ "<select name=\"text-info\" id=\"BARMODE\" data-opt-name=\"barmode\">"
				+ "<option value=\"group\">Group</option>" + "<option value=\"stack\">Stack</option>"
				+ "<option value=\"overlay\">Overlay</option>" + "<option value=\"relative\">Relative</option>"
				+ "</select>" + "</div>" + "</li>";
//        barChart += "<li class=\"general-filters active-filter\" data-column-name=\"BARGAP\" data-key-type=\"layout\">"
//                + "<div class=\"sub-filterItems\">"
//                + "<p>Bar Gap</p>"
//                + "<input type=\"number\" id=\"BARGAP\" data-opt-name=\"bargap\" data-man=\"O\" title=\"Title\"/>"
//                + "</div>"
//                + "</li>";
		barChart += getChartHover("BAR", "layout");
		barChart += getLegendFilters("BAR");
		barChart += getChartColors("BAR");
		barChart += getaxis("BAR", "X");
		barChart += getaxis("BAR", "Y");
		barChart += endUltag;

		String columnChart = startUltag;
		columnChart += getGeneralFilters("COLUMN");
		columnChart += getLabelAndHoverDataFilters("COLUMN");
		columnChart += "<li class=\"general-filters active-filter\" data-column-name=\"COLUMNMODE\" data-key-type=\"layout\">"
				+ "<div class=\"sub-filterItems\">" + "<p>Bar Mode</p>"
				+ "<select name=\"text-info\" id=\"COLUMNMODE\" data-opt-name=\"barmode\">"
				+ "<option value=\"stack\">Stack</option>" + "<option value=\"group\">Group</option>"
				+ "<option value=\"overlay\">Overlay</option>" + "<option value=\"relative\">Relative</option>"
				+ "</select>" + "</div>" + "</li>";
		
		//need to fix
//		columnChart += "<li class=\"general-filters active-filter\" data-column-name=\"COLUMNGAP\" data-key-type=\"layout\">"
//				+ "<div class=\"sub-filterItems\">" + "<p>Bar Gap</p>"
//				+ "<input type=\"number\" id=\"COLUMNGAP\" data-opt-name=\"bargap\" data-man=\"O\" title=\"Title\"/>"
//				+ "</div>" + "</li>";
		columnChart += getChartHover("COLUMN", "layout");
		columnChart += getLegendFilters("COLUMN");
		columnChart += getChartColors("COLUMN");
		columnChart += getaxis("COLUMN", "X");
		columnChart += getaxis("COLUMN", "Y");
		columnChart += endUltag;

		String lineChart = startUltag;
		lineChart += getGeneralFilters("LINES");
		lineChart += "<li class=\"general-filters active-filter\" data-column-name=\"LINESMODE\" data-key-type=\"data\">"
				+ "<div class=\"sub-filterItems\">" + "<label>Mode</label>"
				+ "<select name=\"text-info\" id=\"LINESMODE\" data-opt-name=\"mode\">"
				+ "<option value=\"markers\">Markers</option>" + "<option value=\"lines\" selected>Lines</option>"
				+ "<option value=\"lines+markers\">Lines and Markers</option>"

				//need to fix
				//+ "<option value=\"lines+text\">Lines and Text</option>"
				//+ "<option value=\"lines+markers+text\">Lines, Markers and Text</option>" 
				+"</select>" + "</div>"
				+ "</li>";
		lineChart += getLabelAndHoverDataFilters("LINES");
		lineChart += getChartHover("LINES", "data");
		lineChart += getLegendFilters("LINES");
		lineChart += getaxis("LINES", "X");
		lineChart += getaxis("LINES", "Y");
		lineChart += "<li id=\"marker-filter\" data-column-name=\"LINESMARKER\" data-key-type=\"data\">"
				+ "<div class=\"main-container\">" + "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>"
				+ "<p>Chart Markers</p>" + "</div>" + getToggleButton("LINES", "") + "</div>"
				+ "<ul class=\"sub-filters\" id=\"LINESMARKER\" data-opt-name=\"marker\" style=\"display: none;\">"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"LINESCOLORSMARKER\" data-key-type=\"data\">"
				+ "<label>Marker Color</label>" + "<input type=\"hidden\" id=\"LINESCOLORSMARKER\" value=\"\">"
				+ "<input type=\"color\" id=\"LINESCOLORSMARKER_CLR\" data-opt-name=\"color\" onchange=\"populateSelectedColor(id,'LINESMARKERCOLOR','M')\" value=\"#1864ab\">"
				+ "<div id=\"LINESCOLORSMARKER_CLR_DIV\" class=\"colorsSelectDiv\"></div>"
				// + "<input type=\"color\" id=\"MARKERCOLOR\" data-opt-name=\"color\"
				// data-man=\"O\" title=\"\"/>"
				+ "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"LINESMARKERSIZE\" data-key-type=\"data\">"
				+ "<label>Marker Size</label>"
				+ "<input type=\"number\" id=\"LINESMARKERSIZE\" data-opt-name=\"size\" data-man=\"O\" title=\"\"/>"
				+ "</li>" + "</ul>" + "</li>";
		lineChart += "<li id=\"line-filter\" data-column-name=\"LINES\" data-key-type=\"data\">"
				+ "<div class=\"main-container\">" + "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>"
				+ "<p>Line Markers</p>" + "</div>" + getToggleButton("LINES", "") + "</div>"
				+ "<ul class=\"sub-filters\" id=\"LINES\" data-opt-name=\"line\" style=\"display: none;\">"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"LINESCOLORS\" data-key-type=\"data\">"
				+ "<label>Line Color</label>" + "<input type=\"hidden\" id=\"LINESCOLORS\" value=\"\">"
				+ "<input type=\"color\" id=\"LINESCOLORS_CLR\" data-opt-name=\"color\" onchange=\"populateSelectedColor(id,'LINECOLOR','M')\" value=\"#1864ab\">"
				+ "<div id=\"LINESCOLORS_CLR_DIV\" class=\"colorsSelectDiv\"></div>" + "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"LINESWIDTH\" data-key-type=\"data\">"
				+ "<label>Line Width</label>"
				+ "<input type=\"number\" id=\"LINESWIDTH\" data-opt-name=\"width\" data-man=\"O\" title=\"\"/>"
				+ "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"LINESDASH\" data-key-type=\"data\">"
				+ "<label>Line Dash</label>"
				+ "<select name=\"text-position\" id=\"LINESDASH\" data-opt-name=\"dash\" data-man=\"O\" title=\"\">"
				+ "<option value=\"solid\">Solid</option>" + "<option value=\"dot\">Dot</option>"
				+ "<option value=\"dashdot\">Dashdot</option>" + "<option value=\"longdash\">Longdash</option>"
				+ "</select>" + "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"LINESSHAPE\" data-key-type=\"data\">"
				+ "<label>Line Shape</label>" + "<select name=\"text-info\" id=\"LINESSHAPE\" data-opt-name=\"shape\">"
				+ "<option value=\"linear\">Linear Shape</option>" + "<option value=\"spline\">Spline Shape</option>"
				+ "<option value=\"vh\">VH Shape</option>" + "<option value=\"hvh\">HVH Shape</option>" + "</select>"
				+ "</li>" + "</ul>" + "</li>";
		lineChart += endUltag;

		String bubbleChart = startUltag;
		bubbleChart += getGeneralFilters("SCATTER");
		//bubbleChart += getLabelAndHoverDataFilters("SCATTER");
		bubbleChart += getChartHover("SCATTER", "data");
		bubbleChart += getLegendFilters("SCATTER");
		bubbleChart += getaxis("SCATTER", "X");
		bubbleChart += getaxis("SCATTER", "Y");
		bubbleChart += "<li id=\"line-filter\" data-column-name=\"SCATTERMARKER\" data-key-type=\"data\">"
				+ "<div class=\"main-container\">" + "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>"
				+ "<p>Bubble Markers</p>" + "</div>" + getToggleButton("SCATTER", "") + "</div>"
				
				+ "<ul class=\"sub-filters\" id=\"SCATTERMARKER\" data-opt-name=\"marker\" style=\"display: none;\">"
				//need to fix
				/*
				 * +
				 * "<li class=\"sub-filterItems active-filter\" data-column-name=\"SCATTERCOLORSMARKER\" data-key-type=\"data\">"
				 * + "<label>Bubble Color</label>" +
				 * "<input type=\"hidden\" id=\"SCATTERCOLORSMARKER\" value=\"\">" +
				 * "<input type=\"color\" id=\"SCATTERCOLORSMARKER_CLR\" data-opt-name=\"color\" onchange=\"populateSelectedColor(id,'SCATTERCOLORSMARKER','M')\" value=\"#dce2e8\">"
				 * + "<div id=\"SCATTERCOLORSMARKER_CLR_DIV\" class=\"colorsSelectDiv\"></div>"
				 * + "</li>" +
				 * "<li class=\"sub-filterItems active-filter\" data-column-name=\"SCATTEROPACITY\" data-key-type=\"data\">"
				 * + "<label>Bubble Opacity</label>" +
				 * "<input type=\"number\" id=\"SCATTEROPACITY\" data-opt-name=\"opacity\" data-man=\"O\" title=\"\"/>"
				 * + "</li>"
				 */
				
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"SCATTERMARKERSIZE\" data-key-type=\"data\">"
				+ "<label>Bubble Size</label>"
				+ "<input type=\"number\" id=\"SCATTERMARKERSIZE\" data-opt-name=\"size\" data-man=\"O\" title=\"\"/>"
				+ "</li>" + "</ul>" + "</li>";
		bubbleChart += endUltag;

		String histogramChart = startUltag;
		histogramChart += getGeneralFilters("HISTOGRAM");
		histogramChart += getHoverDataFormat("HISTOGRAM");
		histogramChart += getChartHover("HISTOGRAM", "layout");
		histogramChart += getLegendFilters("HISTOGRAM");
		histogramChart += getChartColors("HISTOGRAM");
		histogramChart += getaxis("HISTOGRAM", "X");
		histogramChart += getaxis("HISTOGRAM", "Y");
		histogramChart += endUltag;

		String funnel = startUltag;
		funnel += getGeneralFilters("FUNNEL");
		funnel += getLabelAndHoverDataFilters("FUNNEL");
		funnel += getChartHover("FUNNEL", "layout");
		funnel += getLegendFilters("FUNNEL");
		funnel += getChartColors("FUNNEL");
		funnel += getaxis("FUNNEL", "X");
		funnel += getaxis("FUNNEL", "Y");
		funnel += endUltag;

		String waterfall = startUltag;
		waterfall += getGeneralFilters("WATERFALL");
		waterfall += getHoverDataFormat("WATERFALL");
		waterfall += getChartHover("WATERFALL", "layout");
		waterfall += getLegendFilters("WATERFALL");
		waterfall += getChartColors("WATERFALL");
		waterfall += getaxis("WATERFALL", "X");
		waterfall += getaxis("WATERFALL", "Y");
		waterfall += endUltag;

		String radar = startUltag;
		radar += getGeneralFilters("SCATTERPOLAR");
		radar += getHoverDataRadar("SCATTERPOLAR");
		radar += getChartHover("SCATTERPOLAR", "layout");
		radar += getLegendFilters("SCATTERPOLAR");
		radar += getChartColors("SCATTERPOLAR");
		radar += getaxis("SCATTERPOLAR", "X");
		radar += getaxis("SCATTERPOLAR", "Y");
		radar += endUltag;

		StringBuilder sunBurst = new StringBuilder();
		sunBurst.append(startUltag);
		sunBurst.append(getTitleFilterECharts("SUNBURST"));
		sunBurst.append(getSliceLabelsECharts("SUNBURST"));
		sunBurst.append(getTooltipDataECharts("SUNBURST"));
		sunBurst.append(endUltag);

		StringBuilder treeMapEcharts = new StringBuilder();
		treeMapEcharts.append(startUltag);
		treeMapEcharts.append(getTitleFilterECharts("TREEMAP"));
		treeMapEcharts.append(getSliceLabelsECharts("TREEMAP"));
		treeMapEcharts.append(getTooltipDataECharts("TREEMAP"));
		treeMapEcharts.append(endUltag);
		
		

		JSONObject filtercolumn = dashBoardsDAO.getcharttableattr(request);
		StringBuilder basicAreaChart = new StringBuilder(startUltag);
		basicAreaChart.append(getGeneralFilters("BASICAREACHART"));
		basicAreaChart.append(getEchartProperties("BASICAREACHART"));
		//basicAreaChart.append(//("BASICAREACHART","S"));
		basicAreaChart.append(getLineColorProperties("BASICAREACHART","S"));
		basicAreaChart.append(endUltag);


		StringBuilder stackedAreaChart = new StringBuilder(startUltag);
		stackedAreaChart.append(getGeneralFilters("STACKEDAREACHART"));
		stackedAreaChart.append(getEchartProperties("STACKEDAREACHART"));
		//stackedAreaChart.append(getChartAreaProperties("STACKEDAREACHART","M"));	
		stackedAreaChart.append(getLineColorProperties("STACKEDAREACHART","M"));
		stackedAreaChart.append(endUltag);


		StringBuilder gradStackAreaChart = new StringBuilder(startUltag);
		gradStackAreaChart.append(getGeneralFilters("GRADSTACKAREACHART"));
		gradStackAreaChart.append(getEchartProperties("GRADSTACKAREACHART"));
		//gradStackAreaChart.append(getChartAreaProperties("GRADSTACKAREACHART","M"));
		gradStackAreaChart.append(getLineColorProperties("GRADSTACKAREACHART","M"));
		gradStackAreaChart.append(endUltag);



		
		StringBuilder areaPiecesChart = new StringBuilder(startUltag);
		areaPiecesChart.append(getAreaPieacesDiv("AREAPIECESCHART"));
			
        areaPiecesChart.append(getGeneralFilters("AREAPIECESCHART"));
		
		areaPiecesChart.append(getEchartProperties("AREAPIECESCHART"));
		
		//areaPiecesChart.append(getChartAreaProperties("AREAPIECESCHART","S"));
		areaPiecesChart.append(getLineColorProperties("AREAPIECESCHART","S"));
		areaPiecesChart.append(endUltag);
		
		
		StringBuilder ganttChart = new StringBuilder(startUltag);
		ganttChart.append(getGeneralFilters("GANTTCHART"));
		
		ganttChart.append(
				  "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + "GANTTCHARTBARHEIGHT" + "\">" + 
				  "<label>Bar Height</label>" + 
				  "<input type=\"number\" id='" + "GANTTCHARTBARHEIGHT" + "' data-opt-name=\"barheight\" data-man=\"O\" title=\"barheight\"/>" +
				  "</li>"
				);
	
		ganttChart.append("<li id=\"marker-filter\" data-column-name=\""+"GANTTCHART"+"AREA\" data-key-type=\"data\">")
		        .append("<div class=\"main-container\"><div class=\"filter-container\">")
		        .append("<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>")
		        .append("<p>Grid Area</p>").append("</div>").append(getToggleButton("GANTTCHART", "")).append("</div>")
		        .append("<ul class=\"sub-filters\" id=\""+"GANTTCHART"+"\" data-opt-name=\"marker\" style=\"display: none;\">")
		        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+"GANTTCHART"+"COLORSAREA\" data-key-type=\"data\">")
		        .append("<label>Grid Area Color</label>").append("<input type=\"hidden\" id=\""+"GANTTCHART"+"COLORSAREA\" value=\"pink\">")
		        .append("<input type=\"color\" id=\""+"GANTTCHART"+"COLORSAREA_CLR\" data-opt-name=\"color\" onchange=\"populateSelectedColor(id,"+"'GANTTCHART"+"AREACOLOR','"+"S"+"')\" value=\"#1864ab\">")
		        .append("<div id=\""+"GANTTCHART"+"COLORSAREA_CLR_DIV\" class=\"colorsSelectDiv\"></div>")
		        .append("</li>")
		        .append("</ul>").append("</li>");
		ganttChart.append(endUltag);
		
		
		StringBuilder candlestick =new StringBuilder(startUltag);
		candlestick.append(getGeneralFilters("CANDLESTICK"));
		candlestick.append(getCandleSticktProperties("CANDLESTICK"));
		candlestick.append(endUltag);
		StringBuilder geochart =new StringBuilder(startUltag);
		geochart.append(getGeneralFilters("GEOCHART"));
		geochart.append(getCandleSticktProperties("GEOCHART"));
		geochart.append(endUltag);


		jsonChartFilter.put("pie", pieChart);
		jsonChartFilter.put("donut", donutChart);
		jsonChartFilter.put("bar", barChart);
		jsonChartFilter.put("column", columnChart);
		jsonChartFilter.put("lines", lineChart);
		jsonChartFilter.put("scatter", bubbleChart);
		jsonChartFilter.put("histogram", histogramChart);
		jsonChartFilter.put("funnel", funnel);
		jsonChartFilter.put("waterfall", waterfall);
		jsonChartFilter.put("scatterpolar", radar);
		jsonChartFilter.put("indicator", gaugeChart);
		jsonChartFilter.put("heatMap", heatMap);
		jsonChartFilter.put("sunburst", sunBurst.toString());
		jsonChartFilter.put("filtercolumn", filtercolumn);
		jsonChartFilter.put("treemap", treeMapEcharts.toString());
		jsonChartFilter.put("BasicAreaChart",basicAreaChart.toString());
		jsonChartFilter.put("StackedAreaChart", stackedAreaChart.toString());
		jsonChartFilter.put("GradStackAreaChart", gradStackAreaChart.toString());
		jsonChartFilter.put("AreaPiecesChart", areaPiecesChart.toString());
		jsonChartFilter.put("ganttChart",ganttChart.toString());  
		jsonChartFilter.put("candlestick", candlestick);
		jsonChartFilter.put("geochart", geochart);
		return jsonChartFilter;
	}

	public String getTitleFilters(String chartType) {
		String generalFilters = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "CHARTTITLE\" data-key-type=\"layout\">" + "<div class=\"sub-filterItems\">" + "<p>Title</p>"
				+ "<input type=\"text\" id=\"" + chartType
				+ "CHARTTITLE\" data-opt-name=\"title\" data-man=\"O\" title=\"Title\"/>" + "</div>" + "</li>";
		return generalFilters;
	}

	public String getGeneralFilters(String chartType) {
		String generalFilters = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "CHARTTITLE\" data-key-type=\"layout\">" + "<div class=\"sub-filterItems\">" + "<p>Title</p>"
				+ "<input type=\"text\" id=\"" + chartType
				+ "CHARTTITLE\" data-opt-name=\"title\" data-man=\"O\" title=\"Title\"/>" + "</div>" + "</li>";
		return generalFilters;
	}

	public String getIndicatorFilters(String chartType) {
		String gaugeChart = "<li class=\"general-filters\" data-column-name=\"" + chartType
				+ "CHARTTITLE\" data-key-type=\"layout\">" + "<div class=\"sub-filterItems\">" + "<p>Title</p>"
				+ "<input type=\"text\" id=\"" + chartType
				+ "CHARTTITLE\" data-opt-name=\"title\" data-man=\"O\" title=\"Title\"/>" + "</div>" + "</li>"
				+ "<li class=\"general-filters\" data-column-name=\"" + chartType
				+ "PAPER_BGCOLOR\" data-key-type=\"layout\">" + "<div class=\"sub-filterItems\">"
				+ "<p>PAPER BGCOLOR</p>" + "<input type=\"color\" id=\"" + chartType
				+ "PAPER_BGCOLOR\" data-opt-name=\"paper_bgcolor\" data-man=\"O\" title=\"Paper Bgcolor\" value=\"#14b1e6\"/>"
				+ "</div>" + "</li>";
		gaugeChart += "<li class=\"legendFontClass\" data-column-name=\"" + chartType
				+ "LEGENDFONT\" data-key-type=\"layout\">" + getFontObject(chartType + "LEGEND", "font") + "</li>";
		return gaugeChart;
	}

	public String getLegendFilters(String chartType) {
		String legendFilters = "<li id=\"legend-filter\" data-column-name=\"" + chartType
				+ "LEGEND\" data-key-type=\"layout\">" + "<div class=\"main-container\">"
				+ "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" + "<p>Legend</p>"
				+ "</div>" + getToggleButton(chartType, "legend") + "</div>" + "<ul class=\"sub-filters\" id=\""
				+ chartType + "LEGEND\" style=\"display: none;\">"
				+ "<li class=\"sub-filterItems\" data-column-name=\"ORIENTATION\">" + "<label>Orientation</label>"
				+ "<select name=\"legend\" id=\"ORIENTATION\" data-opt-name=\"orientation\" data-man=\"O\" title=\"Orientation\">"
				+ "<option v  alue=\"h\">Horizontal</option>" + "<option value=\"v\">Vertical</option>" + "</select>"
				+ "</li>" + "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "SHOWLEGEND\" data-key-type=\"layout\" style=\"display: none;\">" + "<span id=\"" + chartType
				+ "SHOWLEGEND\" data-opt-name=\"showlegend\" value=\"true\"></span>" + "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "LEGENDPOSITION\" data-key-type=\"layout\">" + "<label>Position</label>"
				+ "<select name=\"legend\" id=\"" + chartType + "LEGENDPOSITION\" data-opt-name=\"position\">"
				+ "<option value=\"Top\">Top</option>" + "<option value=\"Bottom\">Bottom</option>"
				+ "<option value=\"Left\">Left</option>" + "<option value=\"Right\" selected>Right</option>"
				+ "</select>" + "</li>" + "<li class=\"legendFontClass active-filter\" data-column-name=\"" + chartType
				+ "LEGENDFONT\">" + getFontObject(chartType + "LEGEND", "font") + "</li>" + "</ul>" + "</li>";
		return legendFilters;
	}

	public String getTreeMapLabelFilters(String chartType) {

		String getTreeMapLabelFilters = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "LABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Data Visible</p>"
				+ "<select name=\"text-info\" id=\"" + chartType + "LABELDATA\" data-opt-name=\"textinfo\">"
				+ "<option value=\"label\">Label</option>" + "<option value=\"value\">Value</option>"
				+ "<option value=\"label+value\">Label and value</option>"
				+ "<option value=\"percent parent\">Parent Percentage</option>"
				+ "<option value=\"label+percent parent\">Label and Parent Percentage</option>"
				+ "<option value=\"label+value+percent parent\">Label,Value and Parent Percentage</option>"
				+ "</select>" + "</div>" + "</li>";
		return getTreeMapLabelFilters;
	}

	public String pieDonutGeneralFilters(String chartType) {

		String pieDonutGeneralFilters = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "LABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Data Visible</p>"
				+ "<select name=\"text-info\" id=\"" + chartType + "LABELDATA\" data-opt-name=\"textinfo\">"
				+ "<option value=\"label\">Label</option>" + "<option value=\"value\">Value</option>"
				+ "<option value=\"label+value\" selected>Label and value</option>"
				+ "<option value=\"percent\">Percentage</option>"
				+ "<option value=\"label+percent\">Label and Percentage</option>"
				+ "<option value=\"value+percent\">Value and Percentage</option>" + "</select>" + "</div>" + "</li>"
				+ "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "LABELPOSITION\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Text Position</p>"
				+ "<select name=\"text-position\" id=\"" + chartType + "LABELPOSITION\" data-opt-name=\"textposition\">"
				+ "<option value=\"inside\">Inside</option>" + "<option value=\"outside\">Outside</option>"
				+ "<option value=\"auto\">Auto</option>" + "<option value=\"none\">None</option>" + "</select>"
				+ "</div>" + "</li>" + "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "HOVERLABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">"
				+ "<p>Hover Data Visible</p>" + "<select name=\"text-info\" id=\"" + chartType
				+ "HOVERLABELDATA\" data-opt-name=\"hoverinfo\" >" + "<option value=\"label\">Label</option>"
				+ "<option value=\"value\">Value</option>" + "<option value=\"percent\">Percentage</option>"
				+ "<option value=\"label+value\">Label and value</option>"
				+ "<option value=\"label+percent\">Label and Percentage</option>"
				+ "<option value=\"value+percent\">Value and Percentage</option>" + "</select>" + "</div>" + "</li>"
				+ "</li>";
		return pieDonutGeneralFilters;
	}

	public String getLabelAndHoverDataFilters(String chartType) {
		String labelAndHoverDataFilters = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "LABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Data Visible</p>"
				+ "<select name=\"text-info\" id=\"" + chartType + "LABELDATA\" data-opt-name=\"textinfo\">"
				+ "<option value=\"''\">None</option>" + "<option value=\"x\">Label</option>"
				+ "<option value=\"y\">Value</option>" + "<option value=\"%\">Percentage</option>"
				+ "<option value=\"x+y\">Label and value</option>"
				+ "<option value=\"x+%\">Label and Percentage</option>"
				+ "<option value=\"y+%\">Value and Percentage</option>" + "</select>" + "</div>" + "</li>"
				+ "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "HOVERLABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">"
				+ "<p>Hover Data Visible</p>" + "<select name=\"text-info\" id=\"" + chartType
				+ "HOVERLABELDATA\" data-opt-name=\"hoverinfo\" >" + "<option value=\"x\">Label</option>"
				+ "<option value=\"y\">Value</option>" + "<option value=\"%\">Percentage</option>"
				+ "<option value=\"x+y\" selected>Label and value</option>"
				+ "<option value=\"x+%\">Label and Percentage</option>"
				+ "<option value=\"value+percent\">Value and Percentage</option>" + "</select>" + "</div>" + "</li>"
				+ "</li>" + "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "LABELPOSITION\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Text Position</p>"
				+ "<select name=\"text-position\" id=\"" + chartType + "LABELPOSITION\" data-opt-name=\"textposition\">"
				+ "<option value=\"inside\">Inside</option>" + "<option value=\"outside\">Outside</option>"
				+ "<option value=\"auto\">Auto</option>" + "<option value=\"none\">None</option>" + "</select>"
				+ "</div>" + "</li>";
		return labelAndHoverDataFilters;
	}

	public String getChartColors(String chartType) {
		String chartColors = "<li id=\"slice-color-filter\" data-column-name=\"" + chartType
				+ "MARKER\" data-key-type=\"data\" style=\"display:none\">" + "<div class=\"main-container\">"
				+ "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" + "<p>Chart Color</p>"
				+ "</div>" + getToggleButton(chartType, "") + "</div>" + "<ul class=\"sub-filters\" id=\"" + chartType
				+ "MARKER\" style=\"display: none;\">" + "<li class=\"sub-filterItems\" data-column-name=\"" + chartType
				+ "COLORS\">" + "<label>Chart Colors</label>" + "<input type=\"hidden\" id=\"" + chartType
				+ "COLORS\" value=\"\">" + "<input type=\"color\" id=\"" + chartType
				+ "COLORS_CLR\" data-opt-name=\"colors\" onchange=\"populateSelectedColor(id,'" + chartType
				+ "COLORS','M')\" value=\"#dce2e8\">" + "<div id=\"" + chartType
				+ "COLORS_CLR_DIV\" class=\"colorsSelectDiv\"></div>" + "</li>"
				+ "<li id=\"slice-color-filter\" data-column-name=\"" + chartType + "LINES\">"
				+ "<div class=\"main-container\">" + "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" + "<p>Line</p>"
				+ "</div>" + getToggleButton(chartType, "") + "</div>" + "<ul class=\"sub-filters\" id=\"" + chartType
				+ "LINES\" style=\"display: none;\">" + "<li class=\"sub-filterItems\" data-column-name=\"" + chartType
				+ "LINECOLOR\">" + "<label>Line Color</label>" + "<input type=\"color\" id=\"" + chartType
				+ "LINECOLOR\" data-opt-name=\"color\">" + "</li>" + "<li class=\"sub-filterItems\" data-column-name=\""
				+ chartType + "LINEWIDTH\">" + "<label>Line Width</label>" + "<input type=\"number\" id=\"" + chartType
				+ "LINEWIDTH\" data-opt-name=\"width\">" + "</li>" + "</ul>" + "</li>" + "</ul>" + "</li>";
		return chartColors;
	}

	public String getTreeMapChartMarkers(String chartType) {
		String chartColors = "<li id=\"slice-color-filter\" data-column-name=\"" + chartType
				+ "MARKER\" data-key-type=\"data\">" + "<div class=\"main-container\">"
				+ "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" + "<p>Marker</p>"
				+ "</div>" + getToggleButton(chartType, "") + "</div>" + "<ul class=\"sub-filters\" id=\"" + chartType
				+ "MARKER\" style=\"display: none;\">" + "<li id=\"slice-color-filter\" data-column-name=\"" + chartType
				+ "LINES\">" + "<div class=\"main-container\">" + "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" + "<p>Line</p>"
				+ "</div>" + getToggleButton(chartType, "") + "</div>" + "<ul class=\"sub-filters\" id=\"" + chartType
				+ "LINES\" style=\"display: none;\">" + "<li class=\"sub-filterItems\" data-column-name=\"" + chartType
				+ "LINECOLOR\">" + "<label>Line Color</label>" + "<input type=\"color\" id=\"" + chartType
				+ "LINECOLOR\" data-opt-name=\"color\">" + "</li>" + "<li class=\"sub-filterItems\" data-column-name=\""
				+ chartType + "LINEWIDTH\">" + "<label>Line Width</label>" + "<input type=\"number\" id=\"" + chartType
				+ "LINEWIDTH\" data-opt-name=\"width\">" + "</li>" + "</ul>" + "</li>" + "</ul>" + "</li>";
		return chartColors;
	}

	public String getHoverDataFormat(String chartType) {
		String hoverDataFormat = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "HOVERLABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">"
				+ "<p>Hover Data Visible</p>" + "<select name=\"text-info\" id=\"" + chartType
				+ "HOVERLABELDATA\" data-opt-name=\" \" >" + "<option value=\"none\">None</option>"
				+ "<option value=\"x\">Label</option>" + "<option value=\"y\">Values</option>"
				+ "<option value=\"x+y\" selected>Label and value</option>" + "</select>" + "</div>" + "</li>";

		return hoverDataFormat;
	}

	public String getHoverTreeDataDataFormat(String chartType) {
		String selected = "selected";
		String hoverDataFormat = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "HOVERLABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">"
				+ "<p>Hover Data Visible</p>" + "<select name=\"text-info\" id=\"" + chartType
				+ "HOVERLABELDATA\" data-opt-name=\"hoverinfo\" >" + "<option value=\"label\" " + selected
				+ ">Label</option>" + "<option value=\"label+value\">Label and value</option>"
				+ "<option value=\"label+value+percent parent\">Label,Value And Parent</option>"
				+ "<option value=\"none\">None</option>" + "</select>" + "</div>" + "</li>";

		return hoverDataFormat;
	}

	public String getHoverDataRadar(String chartType) {
		String hoverDataFormat = "<li class=\"general-filters active-filter\" data-column-name=\"HOVERLABELDATA\" data-key-type=\"data\">"
				+ "<div class=\"sub-filterItems\">" + "<p>Hover Data Visible</p>"
				+ "<select name=\"text-info\" id=\"HOVERLABELDATA\" data-opt-name=\" \" >"
				+ "<option value=\"theta\">Label</option>" + "<option value=\"r\">Values</option>"
				+ "<option value=\"theta+r\" selected>Label and value</option>" + "</select>" + "</div>" + "</li>";

		return hoverDataFormat;
	}

	public String getChartHover(String chartType, String layoutType) {
		String hoverDetails = "<li id=\"slice-hover-filter\" data-column-name=\"" + chartType
				+ "HOVERLABEL\" data-key-type=\"" + layoutType + "\">" + "<div class=\"main-container\">"
				+ "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" + "<p>Chart Hover</p>"
				+ "</div>" + getToggleButton(chartType, "chartHover") + "</div>" + "<ul class=\"sub-filters\" id=\""
				+ chartType + "HOVERLABEL\" style=\"display: none;\">"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + "HOVERBG\">"
				+ "<label>Background Color</label>" + "<input type=\"color\" id=\"" + chartType
				+ "HOVERBG\" data-opt-name=\"bgcolor\" value=\"#74c0fc\">" + "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + "HOVERBORDERCOLOR\">"
				+ "<label>Border Color</label>" + "<input type=\"color\" id=\"" + chartType
				+ "HOVERBORDERCOLOR\" data-opt-name=\"bordercolor\" value=\"#ffffff\">" + "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + "HOVERFONT\">"
				+ getFontObject(chartType + "HOVER", "font") + "</li>" + "</ul>" + "</li>";
		return hoverDetails;
	}

	public String getaxis(String chartType, String axisMode) {

		String axisFilter = "<li id=\"" + axisMode + "-axis-filter\" data-column-name=\"" + chartType + axisMode
				+ "AXIS\" data-key-type=\"layout\">" + "<div class=\"main-container\">"
				+ "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>";
		if (axisMode != null && "X".equalsIgnoreCase(axisMode)) {
			axisFilter += "<p>X-Axis</p>";
		} else if (axisMode != null && "Y".equalsIgnoreCase(axisMode)) {
			axisFilter += "<p>Y-Axis</p>";
		}
		axisFilter += "</div>" + getToggleButton(chartType, axisMode + "axis") + "</div>"
				+ "<ul class=\"sub-filters\" id=\"" + chartType + axisMode + "AXIS\" style=\"display: none;\">"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + axisMode
				+ "AXISTITLE\">" + "<label>Title</label>" + "<input type=\"text\" id='" + chartType + axisMode
				+ "AXISTITLE' data-opt-name=\"title\" data-man=\"O\" title=\"title\"/>" + "</li>"
				// need to fix
//				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + axisMode
//				+ "RANGEMODE\">" + "<label>Range Mode</label>" + "<select id='" + chartType + axisMode
//				+ "RANGEMODE' data-opt-name=\"rangemode\" data-man=\"O\" title=\"RangeMode\">"
//				+ "<option value=\"normal\">Normal</option>" + "<option value=\"tozero\">To Zero</option>"
//				+ "<option value=\"nonnegative\">Non Negative</option>" + "</select>" + "</li>"
				
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + axisMode
				+ "AXISTICKANGEL\">" + "<label>Tick Angle</label>" + "<input type=\"number\" id='" + chartType
				+ axisMode + "AXISTICKANGEL' data-opt-name=\"tickangle\" data-man=\"O\" title=\"tickangle\"/>" + "</li>"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + axisMode
				+ "TITLEFONT\">" + getFontObject(chartType + axisMode + "TITLE", "titlefont") + "</li>" + "</ul>"
				+ "</li>";

		return axisFilter;
	}

	public String getFontObject(String layoutType, String fontKey) {

		String fontObject = "<div class=\"main-container inner-container\">" + 
		"<div class=\"filter-container\">" +
		"<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" +
		"<p>Font</p>" +
		"</div>" +
		"</div>" +
		"<ul id=\"" + layoutType + "FONT\" data-opt-name=\"" + fontKey +
		"\" data-man=\"O\" style=\"display: none\">" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + layoutType + "FONTCOLOR\">" +
		"<label>Font Color</label>" +
		"<input type=\"color\" id=\"" + layoutType +
		"FONTCOLOR\" data-opt-name=\"color\" data-man=\"O\" value=\"#343a40\"/>" +
		"</li>" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + layoutType + "FONTFAMILY\">" +
		"<label>Font Family</label>" +
		"<select id=\"" + layoutType + "FONTFAMILY\" data-opt-name=\"family\" data-man=\"O\">" +
		"<option value=\"Arial, sans-serif\">Arial</option>" +
		"<option value=\"Verdana, sans-serif\">Verdana</option>" +
		"<option value=\"Tahoma, sans-serif\">Tahoma</option>" +
		"<option value=\"Georgia, serif\">Georgia</option>" +
		"<option value=\"Times New Roman, serif\">Times New Roman</option>" +
		"<option value=\"Courier New, monospace\">Courier New</option>" +
		"<option value=\"'Apple System', sans-serif\">Apple System</option>" +
		"<option value=\"'Segoe UI', sans-serif\">Segoe UI</option>" +
		"</select>" +
		"</li>" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + layoutType + "FONTSIZE\">" +
		"<label>Font Size</label>" +
		"<input type=\"number\" id=\"" + layoutType + "FONTSIZE\" data-opt-name=\"size\" data-man=\"O\"/>" +
		"</li>" +
		"</ul>";
		return fontObject;
	}

	public String getToggleButton(String chartName, String filterType) {

		String toggleButton = "<div class=\"toggle-container\">" + "<div id=\"toggleButtonFor" + filterType + chartName
				+ "\" class=\"toggle-btn active\">" + "<span class=\"on-off-text\">on</span>"
				+ "<div class=\"straight-line\">&nbsp;</div>" + "<div class=\"circle-bg\">&nbsp;</div>" + "</div>"
				+ "</div>";
		return toggleButton;
	}

	public String getTitleFilterECharts(String chartType) {
		String generalFilters = "<li class=\"general-filters active-filter\" data-column-name=\"" + chartType
				+ "TITLEECHARTS\" data-key-type=\"layout\">" + "<div class=\"sub-filterItems\">" + "<p>Title</p>"
				+ "<input type=\"text\" id=\"" + chartType
				+ "TITLEECHARTS\" data-opt-name=\"text\" data-man=\"O\" title=\"Title\"/>" + "</div>" + "</li>";
		return generalFilters;
	}

	public String getSliceLabelsECharts(String chartType) {
		String sliceLabels = "<li id=\"label-filter\" data-column-name=\"" + chartType
				+ "SLICELABELECHARTS\" data-key-type=\"data\">" + "<div class=\"main-container\">"
				+ "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>"
				+ "<p>Slice Label</p>" + "</div>" + getToggleButton("SUNBURST", "") + "</div>"
				+ "<ul class=\"sub-filters\" id=\"" + chartType
				+ "SLICELABELECHARTS\" data-opt-name=\"label\" style=\"display: none;\">"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "SLICELABELDATAECHARTS\" data-key-type=\"data\">" + "<label>Label Data</label>" + "<select id=\""
				+ chartType
				+ "SLICELABELDATAECHARTS\" data-opt-name=\"formatter\" data-man=\"O\" title=\"Chart Hover Data\">"
				+ "<option value=\"getLabelFormatter\">Label</option>"
				+ "<option value=\"getValueFormatter\">Value</option>"
				+ "<option value=\"getLabelAndValueLabelFormatter\" selected>Label and Value</option>" + "</select>"
				+ "</li>" + "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "LABELROTATEECHARTS\" data-key-type=\"data\">" + "<label>Rotate</label>"
				+ "<input type=\"number\" id=\"" + chartType
				+ "LABELROTATEECHARTS\" data-opt-name=\"rotate\" value=\"0\" title= \"Rotation from -90 degrees to 90 degrees, Positive values stand for counterclockwise\" data-man=\"O\"/>"
				+ "</li>" + "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "LABELPOSITIONECHARTS\" data-key-type=\"data\">" + "<label>Position</label>"
				+ "<select name=\"legend\" id=\"" + chartType
				+ "LABELPOSITIONECHARTS\" data-opt-name=\"position\" data-man=\"O\" title=\"Position\">"
				+ "<option value=\"inside\">Inside</option>" + "<option value=\"outside\">Outside</option>"
				+ "</select>" + "</li>" + "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "LABELFONTWIDTHECHARTS\" data-key-type=\"data\">" + "<label>Label Width</label>"
				+ "<input type=\"number\" id=\"" + chartType
				+ "LABELFONTWIDTHECHARTS\" data-opt-name=\"width\" value=\"40\" data-man=\"O\"/>" + "</li>"

		//need to fix
//				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
//				+ "LABELOVERFLOWECHARTS\" data-key-type=\"data\">" + "<label>Overflow</label>"
//				+ "<select name=\"legend\" id=\"" + chartType
//				+ "LABELOVERFLOWECHARTS\" data-opt-name=\"overflow\" data-man=\"O\" title=\"Position\">"
//				+ "<option value=\"truncate\">Truncate</option>" + "<option value=\"break\">Break</option>"
//				+ "<option value=\"breakAll\">Break All</option>" + "<option value=\"none\">None</option>" + "</select>"
//				+ "</li>" 
				
				+ getFontListForECharts(chartType) + "</ul>" + "</li>";
		return sliceLabels;

	}

	public String getFontListForECharts(String chartType) {
		String fontList = "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType +
		"LABELFONTCOLORECHARTS\" data-key-type=\"data\">" +
		"<label>Color</label>" +
		"<input type=\"color\" id=\"" + chartType + "LABELFONTCOLORECHARTS\" data-opt-name=\"color\" value=\"#333\">" +
		"</li>" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType +
		"LABELFONTSIZEECHARTS\" data-key-type=\"data\">" +
		"<label>Font Size</label>" +
		"<input type=\"number\" id=\"" + chartType + "LABELFONTSIZEECHARTS\" data-opt-name=\"size\" value=\"12\" data-man=\"O\"/>" +
		"</li>" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType +
		"LABELFONTFAMILYECHARTS\" data-key-type=\"data\">" +
		"<label>Font Family</label>" +
		"<select name=\"legend\" id=\"" + chartType + "LABELFONTFAMILYECHARTS\" data-opt-name=\"fontFamily\" data-man=\"O\" title=\"Font Family\">" +
		"<option value=\"sans-serif\">Sans Serif</option>" +
		"<option value=\"serif\">Serif</option>" +
		"<option value=\"monospace\">Monospace</option>" +
		"<option value=\"Arial\">Arial</option>" +
		"<option value=\"Courier New\">Courier New</option>" +
		"<option value=\"'Apple System', sans-serif\">Apple System</option>" + // Added Apple System
		"<option value=\"'Segoe UI', sans-serif\">Segoe UI</option>" + // Added Segoe UI
		"</select>" +
		"</li>";
		return fontList;
	}

	public String getTooltipDataECharts(String chartType) {

		String tooltip = "<li id=\"label-filter\" data-column-name=\"" + chartType
				+ "TOOLTIPECHARTS\" data-key-type=\"layout\">" + "<div class=\"main-container\">"
				+ "<div class=\"filter-container\">"
				+ "<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>"
				+ "<p>Chart Hover</p>" + "</div>" + getToggleButton("SUNBURST", "") + "</div>"
				+ "<ul class=\"sub-filters\" id=\"" + chartType
				+ "TOOLTIPECHARTS\" data-opt-name=\"tooltip\" style=\"display: none;\">"
				+ "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "HOVERDATAECHARTS\" data-key-type=\"data\">" + "<label>Hover Data</label>" + "<select id=\""
				+ chartType
				+ "HOVERDATAECHARTS\" data-opt-name=\"formatter\" data-man=\"O\" title=\"Chart Hover Data\">"
				+ "<option value=\"getLabelFormatter\">Label</option>"
				+ "<option value=\"getValueFormatter\">Value</option>"
				+ "<option value=\"getLabelAndValueTooltipFormatter\" selected>Label and Value</option>" + "</select>"
				+ "</li>" + "<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType
				+ "BACKGROUNDCOLORECHARTS\" data-key-type=\"layout\">" + "<label>Background Color</label>"
				+ "<input type=\"color\" id=\"" + chartType
				+ "BACKGROUNDCOLORECHARTS\" data-opt-name=\"backgroundColor\" value=\"#333\">" + "</li>"
				+ "<li class=\"sub-filters\" data-column-name=\"" + chartType
				+ "TEXTSTYLEECHARTS\" data-key-type=\"layout\">" + getFontObjectEcharts(chartType) + "</li>" + "</ul>";
		return tooltip;
	}

	public String getFontObjectEcharts(String chartType) {
		String fontObject = "<div class=\"main-container inner-container\">" +
		"<div class=\"filter-container\">" +
		"<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons\"/>" +
		"<p>Font</p>" +
		"</div>" +
		"</div>" +
		"<ul id=\"" + chartType + "TEXTSTYLEECHARTS\" data-opt-name=\"textStyle\" data-man=\"O\" style=\"display: none;\">" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + "FONTCOLORECHARTS\" data-key-type=\"layout\">" +
		"<label>Color</label>" +
		"<input type=\"color\" id=\"" + chartType + "FONTCOLORECHARTS\" data-opt-name=\"color\" value=\"#74c0fc\">" +
		"</li>" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + "FONTSIZEECHARTS\" data-key-type=\"layout\">" +
		"<label>Font Size</label>" +
		"<input type=\"number\" id=\"" + chartType + "FONTSIZEECHARTS\" data-opt-name=\"size\" data-man=\"O\"/>" +
		"</li>" +
		"<li class=\"sub-filterItems active-filter\" data-column-name=\"" + chartType + "FONTFAMILYECHARTS\" data-key-type=\"layout\">" +
		"<label>Font Family</label>" +
		"<select name=\"legend\" id=\"" + chartType + "FONTFAMILYECHARTS\" data-opt-name=\"fontFamily\" data-man=\"O\" title=\"Font Family\">" +
		"<option value=\"sans-serif\">Sans Serif</option>" +
		"<option value=\"serif\">Serif</option>" +
		"<option value=\"monospace\">Monospace</option>" +
		"<option value=\"Arial\">Arial</option>" +
		"<option value=\"Courier New\">Courier New</option>" +
		"<option value=\"'Apple System', sans-serif\">Apple System</option>" + // Added Apple System
		"<option value=\"'Segoe UI', sans-serif\">Segoe UI</option>" + // Added Segoe UI
		"</select>" +
		"</li>" +
		"</ul>";
		return fontObject;
	}

	public JSONObject chartJoinTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray tablesObj = new JSONArray();
		// JSONObject labelObj = new VisionUtills().getMultilingualObject(request);
		String fromTable = request.getParameter("tablesObj");
		if (fromTable != null && !"".equalsIgnoreCase(fromTable) && !"null".equalsIgnoreCase(fromTable)) {    
			tablesObj = (JSONArray) JSONValue.parse(fromTable);
		}
		try {
			String tabsString = "<div id='dataMigrationTabs' class='dataMigrationTabs'>"
					+ "<div id='tabs-1' class='dataMigrationsTabsInner'>" 
					+ "</div>" 
					+ "</div>" 
					+ "<div id='viewMergeJoinQueryDivId' class='viewMergeJoinQueryDivClass'></div>"
			        + "<div id='viewMergeJoinQueryErrorDivId' class='viewMergeJoinQueryErrorDivClass'></div>"
			        + "<input type='hidden' id='userEditorMergeJoinSaveId' value='false'/>";
			resultObj.put("tabsString", tabsString);
			resultObj.put("selectedJoinTables", joinDxpTransformationRules(request, tablesObj));      
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public String joinDxpTransformationRules(HttpServletRequest request, JSONArray tablesObj) {
		String joinTableString = "";
		try {
			if (tablesObj != null && !tablesObj.isEmpty()) {
				JSONArray sourceTablesArr = new JSONArray();
				sourceTablesArr.addAll(tablesObj);
				//
				if (sourceTablesArr != null && sourceTablesArr.size() > 1) {
					joinTableString += "<div class=\"visionEtlMappingMain\">" + ""
							+ "<div class=\"visionEtlMappingTablesDiv VisionAnalyticMappingTables\">"
							+ "<table class=\"visionEtlMappingTables\" id='EtlMappingTable'" + ">" + "<thead>"
							+ "<tr><th style='background: #f1f1f1 none repeat scroll 0 0;text-align: center' colspan=\"2\">Tables</th>";

					for (int i = 0; i < sourceTablesArr.size(); i++) {
						joinTableString += "<tr><td class=\"sourceJoinColsTd\">"
								+ "<select id=\"SOURCE_SELECT_JOIN_TABLES_" + i
								+ "\" onchange=changeSelectedTableDb(id," + i + ")  class=\"sourceColsJoinSelectBox\""
								+ ">" + "" + generateTableSelectBoxStr(sourceTablesArr, (String) sourceTablesArr.get(i),
										"SOURCE_SELECT_JOIN_TABLES_" + i + "")
								+ "" + "</select>" + "</td>" + "<td>";
						if (i != 0) {
							joinTableString += "<img src=\"images/mapping.svg\" " + " id=\"joinConditionsMap_" + i
									+ "\" "
									+ "class=\"visionEtlMapTableIcon visionEtlJoinClauseMapIcon\" title=\"Map Columns For Join\""
									+ " onclick=showDxpJoinsTables(event,'" + sourceTablesArr.get(i) + "',id," + i + ")"
									+ " style=\"width:15px;height: 15px;cursor:pointer;\"/>";
						}
						joinTableString += "</td>" + "</tr>";

					}

					joinTableString += "</tbody>" + "" + "</table>" + "</div>"
							+ "<div id=\"joinMapColumnsDivId\" class=\"joinMapColumnsDivClass\"></div>" + "</div>";

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return joinTableString;
	}

	public String generateTableSelectBoxStr(List<String> columnList, String selectedTable, String selectBoxId) {
		String selectBoxStr = "<option>Select</option>";
		try {
			for (int i = 0; i < columnList.size(); i++) {
				String table = columnList.get(i);
				String selectedStr = "";
				if (selectedTable != null && !"".equalsIgnoreCase(selectedTable)
						&& selectedTable.equalsIgnoreCase(String.valueOf(table))) {
					selectedStr = "selected";
				}
				selectBoxStr += "<option  value='" + table + "'" + " id ='" + selectBoxId + "_" + table
						+ "' data-tablename='" + table + "' " + "" + selectedStr + ">" + table + "</option>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return selectBoxStr;
	}

	public JSONObject fetchJoinTableColumnTrfnRules(HttpServletRequest request) {
		String joinsDataStr = "";
		JSONObject resultObj = new JSONObject();
		Connection connection = null;
		try {
			// ravi start
			String joinType = "";
			// ravi end

			JSONArray masterTablesArray = new JSONArray();
			String dbObjStr = request.getParameter("dbObj");
			String childTableName = request.getParameter("tableName");
			if (childTableName != null && !"".equalsIgnoreCase(childTableName)
					&& !"null".equalsIgnoreCase(childTableName) && childTableName.contains(".")) {
				childTableName = childTableName.substring(childTableName.lastIndexOf(".") + 1);
			}
			String masterTables = request.getParameter("sourceTables");
			if (masterTables != null && !"".equalsIgnoreCase(masterTables) && !"".equalsIgnoreCase(masterTables)) {
				masterTablesArray = (JSONArray) JSONValue.parse(masterTables);
			}
			String joinColumnMapping = request.getParameter("joinColumnMapping");
			JSONObject joinColumnMappingObj = new JSONObject();
			if (joinColumnMapping != null && !"".equalsIgnoreCase(joinColumnMapping)
					&& !"null".equalsIgnoreCase(joinColumnMapping)) {
				joinColumnMappingObj = (JSONObject) JSONValue.parse(joinColumnMapping);
			}
			// String trString = "<tr>";
			List<Object[]> childTableColumnList = new ArrayList<>();
			childTableColumnList = dashBoardsDAO.getTreeOracleTableColumns(request, childTableName);

			JSONArray childTableColsTreeArray = new JSONArray();
			if (childTableColumnList != null && !childTableColumnList.isEmpty()) {
				JSONObject tableObj = new JSONObject();
				tableObj.put("id", childTableName);// CONNECTION_NAME
				tableObj.put("text", childTableName);
				tableObj.put("value", childTableName);
				tableObj.put("icon", "images/GridDB.png");
				childTableColsTreeArray.add(tableObj);
				for (int i = 0; i < childTableColumnList.size(); i++) {
					Object[] childColsArray = childTableColumnList.get(i);
					if (childColsArray != null && childColsArray.length != 0) {
						JSONObject columnObj = new JSONObject();
						columnObj.put("id", childColsArray[0] + ":" + childColsArray[1]);
						columnObj.put("text", childColsArray[1]);
						columnObj.put("value", childColsArray[0] + ":" + childColsArray[1]);
						columnObj.put("parentid", childColsArray[0]);
						childTableColsTreeArray.add(columnObj);
					}

				}
			}
			resultObj.put("childTableColsArray", childTableColsTreeArray);

			// ravi start
			String trString = "<tr>";
			String singleTrString = "<tr>";
			singleTrString += "<td width='5%'><img src=\"images/Detele Red Icon.svg\" onclick='deleteSelectedRow(this)'  class=\"visionTdETLIcons\""
					+ " title=\"Delete\" style=\"width:15px;height: 15px;cursor:pointer;\"/>" + "</td>";
			singleTrString += "<td width='35%' class=\"sourceJoinColsTd\"><input class='visionColJoinMappingInput' type='text' value='' readonly='true'/>"
					+ "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
					+ " onclick=\"selectDxpColumn(this,'childColumn')\" style=\"\"></td>";

			singleTrString += "<td width='10%' class=\"sourceJoinColsTd\">"
					+ "<select id=\"OPERATOR_TYPE\"  class=\"sourceColsJoinSelectBox\">"
					+ "<option  value='=' selected>=</option>" + "<option  value='!='>!=</option>" + "</select>"
					+ "</td>";

			singleTrString += "<td width='35%' class=\"sourceJoinColsTd\"><input class='visionColJoinMappingInput' type='text' value='' readonly='true'/>"
					+ "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
					+ " onclick=\"selectDxpColumn(this,'masterColumn')\" style=\"\"></td>";
			singleTrString += "<td width='10%'><input type=\"text\" class=\"defaultValues\" id=\"static_value_0\"></td>"
					+ "<td width='5%'>" + "<select id='andOrOpt'>" + "<option value='AND'>AND</option>"
					+ "<option value='OR'>OR</option>" + "</select>" + "</td>";
			singleTrString += "</tr>";

			// ravi end
			// ravi start
			JSONArray masterTableColsTreeArray = new JSONArray();
			for (int i = 0; i < masterTablesArray.size(); i++) {
				String masterTableName = (String) masterTablesArray.get(i);
//                if (request.getParameter("tableName") != null && !"".equalsIgnoreCase(request.getParameter("tableName"))
//                        && !childTableName.equalsIgnoreCase(request.getParameter("tableName"))) {
				if (masterTableName != null && !"".equalsIgnoreCase(masterTableName)
						&& !"null".equalsIgnoreCase(masterTableName) && masterTableName.contains(".")) {
					masterTableName = masterTableName.substring(masterTableName.lastIndexOf(".") + 1);
				}
				List<Object[]> columnList = new ArrayList<>();
				columnList = dashBoardsDAO.getTreeOracleTableColumns(request, masterTableName);

				if (columnList != null && !columnList.isEmpty()) {
					JSONObject tableObj = new JSONObject();
					tableObj.put("id", masterTableName);
					tableObj.put("text", masterTableName);
					tableObj.put("value", masterTableName);
					tableObj.put("icon", "images/GridDB.png");
					masterTableColsTreeArray.add(tableObj);
					for (int j = 0; j < columnList.size(); j++) {
						Object[] masterColsArray = columnList.get(j);
						if (masterColsArray != null && masterColsArray.length != 0) {
							JSONObject columnObj = new JSONObject();
							columnObj.put("id", masterColsArray[0] + ":" + masterColsArray[1]);
							columnObj.put("text", masterColsArray[1]);
							columnObj.put("value", masterColsArray[0] + ":" + masterColsArray[1]);
							columnObj.put("parentid", masterColsArray[0]);
							masterTableColsTreeArray.add(columnObj);
						}

					}
				}
//                }
			}
			resultObj.put("masterTableColsArray", masterTableColsTreeArray);
			trString = singleTrString;

// ravi end 
			// String joinType = "";
			String mappedColTrString = "";
			if (joinColumnMappingObj != null && !joinColumnMappingObj.isEmpty()) {
				Set keySet = joinColumnMappingObj.keySet();
				List keysList = new ArrayList();
				keysList.addAll(keySet);
				Collections.sort(keysList);

				for (int i = 0; i < keysList.size(); i++) {
					Object keyName = keysList.get(i);
					JSONObject joinColMapObj = (JSONObject) joinColumnMappingObj.get(keysList.get(i));
					if (joinColMapObj != null && !joinColMapObj.isEmpty()) {
						joinType = (String) joinColMapObj.get("joinType");
						mappedColTrString += "<td width='5%' ><img src=\"images/Detele Red Icon.svg\" onclick='deleteSelectedRow(this)'  class=\"visionTdETLIcons\""
								+ " title=\"Delete\" style=\"width:15px;height: 15px;cursor:pointer;\"/>" + "</td>";
						mappedColTrString += "<td width='35%' class=\"sourceJoinColsTd\">"
								+ "<input class='visionColJoinMappingInput' type='text' value='"
								+ (String) joinColMapObj.get("childTableColumn") + "' readonly='true'/>"
								+ "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
								+ " onclick=\"selectDxpColumn(this,'childColumn')\" style=\"\"></td>";

						String operator = (String) joinColMapObj.get("operator");

						mappedColTrString += "<td width='10%' class=\"sourceJoinColsTd\">"
								+ "<select id=\"OPERATOR_TYPE\"  class=\"sourceColsJoinSelectBox\">";
						mappedColTrString += "<option  value='=' " + ("=".equalsIgnoreCase(operator) ? "selected" : "")
								+ ">=</option>";
						mappedColTrString += "<option  value='!=' "
								+ ("!=".equalsIgnoreCase(operator) ? "selected" : "") + ">!=</option>";
						mappedColTrString += "</select>" + "</td>";
						mappedColTrString += "<td width='35%' class=\"sourceJoinColsTd\">"
								+ "<input class='visionColJoinMappingInput' type='text' value='"
								+ (String) joinColMapObj.get("masterTableColumn") + "' readonly='true'/>"
								+ "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
								+ " onclick=\"selectDxpColumn(this,'masterColumn')\" style=\"\"></td>";
						mappedColTrString += "" + "<td width='10%'><input type=\"text\" " + "value='"
								+ ((joinColMapObj.get("staticValue") != null
										&& !"".equalsIgnoreCase(String.valueOf(joinColMapObj.get("staticValue")))
										&& !"null".equalsIgnoreCase(String.valueOf(joinColMapObj.get("staticValue"))))
												? String.valueOf(joinColMapObj.get("staticValue"))
												: "")
								+ "' " + " class=\"defaultValues\" id=\"static_value_" + i + "\"></td>"
								+ "<td width='5%'>" + "<select id='andOrOpt'>";
						String andOrOperator = (String) joinColMapObj.get("andOrOperator");
						mappedColTrString += "<option value='AND' "
								+ ("AND".equalsIgnoreCase(andOrOperator) ? "selected" : "") + ">AND</option>";
						mappedColTrString += "<option value='OR' "
								+ ("OR".equalsIgnoreCase(andOrOperator) ? "selected" : "") + ">OR</option>";
						mappedColTrString += "</select>" + "</td>";
						mappedColTrString += "</tr>";
					}

				}
			}
			if (!(mappedColTrString != null && !"".equalsIgnoreCase(mappedColTrString)
					&& !"null".equalsIgnoreCase(mappedColTrString))) {
				mappedColTrString = trString;
			}
			joinsDataStr += "<div class=\"visionEtlJoinClauseMain visionAnalyticsJoinClauseMain\">"
					+ "<div class=\"visionEtlAddIconDiv\">"
					+ "<img data-trstring='' src=\"images/Add icon.svg\" id=\"visionDxpAddRowIcon\" "
					+ "class=\"visionDxpAddRowIcon\" title=\"Add column for mapping\""
					+ " onclick=addNewDxpJoinsRow(event,'" + dbObjStr + "',id) "
					+ "style=\"width:15px;height: 15px;cursor:pointer; float: left;\"/>"
					+ "<img data-trstring='' src=\"images/Save Icon.svg\" id=\"visionEtlSaveIcon\" "
					+ "class=\"visionDxpAddRowIcon\" title=\"Save Mapping\"" + " onclick=saveDxpJoinMapping(event,id) "
					+ "style=\"width:15px;height: 15px;cursor:pointer; float: left;\"/>"
					+ "<span class='visionDxpColumnJoinType'>Join Type : </span>"
					+ "<select class='visionDxpColumnJoinType' id='joinType'>" + "<option value='INNER JOIN' "
					+ ("INNER JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + " >Inner Join</option>"
					+ "<option value='JOIN' " + ("JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + ">Join</option>"
					+ "<option value='LEFT OUTER JOIN' "
					+ ("LEFT OUTER JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + ">Left Outer Join</option>"
					+ "<option value='RIGHT OUTER JOIN' "
					+ ("RIGHT OUTER JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + ">Right Outer Join</option>"
					+ "<option value='OUTER JOIN' " + ("OUTER JOIN".equalsIgnoreCase(joinType) ? "selected" : "")
					+ ">Outer Join</option>" + "</select>" + "</div>" + "<div class=\"visionDxpJoinClauseTablesDiv\">"
					+ "<table class=\"visionEtlJoinClauseTable\" id='etlJoinClauseTable' style='width: 100%;' border='1'>"
					+ "<thead>" + "<tr>"
					+ "<th width='5%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'></th>"
					+ "<th width='35%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Child Column</th>"
					+ "<th width='10%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Operator</th>"
					+ "<th width='35%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Master Column</th>"
					+ "<th width='10%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Static Value</th>"
					+ "<th width='5%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>AND/OR</th>"
					+ "" + "</tr>" + "</thead>" + "<tbody>" + "";
			joinsDataStr += mappedColTrString + "</tbody>" + "" + "</table>" + "" + "</div>" + "</div>";
			resultObj.put("joinsDataStr", joinsDataStr);
			resultObj.put("trString", singleTrString); // ravi edit

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {
			}
		}
		return resultObj;

	}

	public JSONObject fetchCardDetails(HttpServletRequest request) {
		return dashBoardsDAO.fetchCardDetails(request);
	}

	public JSONObject fetchHomeCardDetails(HttpServletRequest request) {
		return dashBoardsDAO.fetchHomeCardDetails(request);
	}

	public JSONObject fetchpredictiveChartData(HttpServletRequest request) {
		return dashBoardsDAO.fetchpredictiveChartData(request);
	}

	public String saveVisualizeData(HttpServletRequest request) { 
		return dashBoardsDAO.saveVisualizeData(request);
	}

	public String getchartElement(HttpServletRequest request) {

		String result = "<div class='searchedDxpSearchResults' id='searchedDxpSearchResults'>";
		int Count = 0;
		String className = "";
		try {

			String typedValue = request.getParameter("typedValue");
			String domainValue = request.getParameter("domainValue");
			String chartid = request.getParameter("chartid");
			// String count = request.getParameter("count");
			List resultList = new ArrayList();
			resultList.add("Axex");
			resultList.add("Axex Titles");
			resultList.add("Chart Titles");
			resultList.add("Data Lebel");
			resultList.add("Data Table");
			resultList.add("Trendline");

			if (resultList != null && !resultList.isEmpty()) {
				for (int i = 0; i < resultList.size(); i++) {
					String name = (String) resultList.get(i);
					result += "<div class='searchFilterResultsList'>"
							// + "<input type='checkbox' name='dxpFilterSearchCheckBox'
							// class='dxpFilterSearchCheckClass' id='dxpFilterSearchCheckId' value='" + name
							// + "'/>"
							+ "<span class=\"chartElementPopContentTitle\">" + name + "</span>"
							+ "<div id='chartelementId" + Count
							+ "' class='chartElementImgClass'> <img onclick=\"getChartContent('chartelementId" + Count
							+ "','" + chartid + "')\"  src='images/nextrightarrow.png' title='next row' />" + "</div>"
							+ "</div>";
					Count++;

				}
			}

			result += "</div>";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String getchartchildElement(HttpServletRequest request) {
		String result = "<div class='searchedDxpSearchResults' id='searchedDxpSearchResults'>";
		int Count = 0;
		String className = "";
		try {
			String typedValue = request.getParameter("typedValue");
			String domainValue = request.getParameter("domainValue");
			String chartid = request.getParameter("chartid");
			String chartType = request.getParameter("chartType");
			String count = request.getParameter("count");
			JSONObject movingAvgObj = new JSONObject();
			movingAvgObj.put("Linear", "L");
			movingAvgObj.put("Exponential", "E");
			movingAvgObj.put("Moving avgerage", "M");
			for (Object key : movingAvgObj.keySet()) {
				String keyStr = (String) key;
				Object keyvalue = movingAvgObj.get(keyStr);
				result += "<div class='searchFilterResultsList' onclick=\"getpredictivechart(event,'" + chartid + "','"
						+ chartType + "','" + count + "','" + keyvalue + "')\">"
						+ "<div class='chartElementPopContentTitle1'>" + keyStr + "</div>" + "</div>";
			}
			result += "</div>";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String getButtons() {
		JSONObject buttonobj = new JSONObject();
		String result = "<span>"
				/*
				 * +
				 * "<button type='button' value='Emport' class='emportclasss btn ' onclick=\"showVisuvalization()\">Import</button>"
				 * +
				 * "<button type='button' value='Save' class='Saveclasss btn ' onclick=\"saveVisualizationData()\">Save</button>"
				 * +
				 * "<button type='button' value='Analysis' class='Analysisclasss btn ' onclick=\"showSlicerField1()\">Analysis</button>"
				 * +
				 * "<button type='button' value='Format' class='Formateclasss btn ' onclick=\"showSlicerField1()\">Format</button>"
				 */
				// + "<button type='button' value='IntelliSense' class='Analysisclasss btn '
				// onclick=\"showIntelliSenseSuggestions()\">IntelliSense</button>"
				+ "</span>";
		return result;
	}

	public JSONObject getChartData(HttpServletRequest request) {
		return dashBoardsDAO.getChartData(request);
	}

	public JSONObject getfilterColumnData(HttpServletRequest request) {   
		String Resultstr = "";
		JSONObject resultobj = new JSONObject();
//        String tablename = request.getParameter("table");
		try {
			// JSONObject filtercolumn = dashBoardsDAO.getcharttableattr(request);
			String result = dashBoardsDAO.getLoadTableColumns(request);
			JSONObject savedFilterObj = dashBoardsDAO.getSaveFilterColumns(request);
			Resultstr = "<div id='FilterColumndataId' class = 'FilterColumndataClass'>"
					+ "<div id=\"VisualizeBIFilterColumns\"></div>"
					+ "<div id=\"visualizeChartConfigFiltersData\" class=\"visualizeChartConfigFiltersClass\"></div>"
					+ "</div>";

			resultobj.put("Resultstr", Resultstr);
			resultobj.put("result", result);
			resultobj.put("savedFilterObj", savedFilterObj);
			// resultobj.put("filtercolumn", filtercolumn);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;

	}

	public JSONObject getchartGrid(HttpServletRequest request) {
		JSONObject gridObj = new JSONObject();
		try {
			String gridId = request.getParameter("gridId");
			gridObj = dashBoardsDAO.getGrid(gridId, request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gridObj;
	}

	public List getSerachResults(List gridColArray, String tableName, String gridId, HttpServletRequest request) {
		List dataList = new ArrayList();
		try {
			JSONArray paramArray = new JSONArray();
			String paramArrayStr = request.getParameter("paramArray");
			if (paramArrayStr != null && !"".equalsIgnoreCase(paramArrayStr)) {
				paramArray = (JSONArray) JSONValue.parse(paramArrayStr);
			}
			dataList = cloudGridResultsDAO.getSerachResults(gridColArray, tableName, gridId, request, paramArray);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataList;
	}

	public String deleteVisualizeChart(HttpServletRequest request) {
		return dashBoardsDAO.deleteVisualizeChart(request);
	}

	public String create(HttpServletRequest request) {
		String result = "<div class='searchedDxpSearchResults' id='searchedDxpSearchResults'>";
		int Count = 0;
		String className = "";
		try {
			String chartid = request.getParameter("chartid");
			// String count = request.getParameter("count");
			List resultList = new ArrayList();
			resultList.add("Single");
			resultList.add("Multiple");
			if (resultList != null && !resultList.isEmpty()) {
				for (int i = 0; i < resultList.size(); i++) {
					String name = (String) resultList.get(i);
					result += "<div class='createpopupResultsList' onclick=\"callElements(event)\">"
							// + "<input type='checkbox' name='dxpFilterSearchCheckBox'
							// class='dxpFilterSearchCheckClass' id='dxpFilterSearchCheckId' value='" + name
							// + "'/>"
							+ "<span class=\"chartElementPopContentTitle\">" + name + "</span>" + "</div>";
					Count++;

				}
			}

			result += "</div>";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String updatechartdata(HttpServletRequest request) {
		return dashBoardsDAO.updatechartdata(request);
	}

	public String dashboardSetting(HttpServletRequest request) {
		String result = "";
		try {

			result = "<div class='dxpDataAnalyticswrapper'>"
					+ "<div id=\"columnsMainDivwrapperID\" class='columnsMainDivwrapper width60'>"
					+ "<div id=\"visualizationMainDivwrapperID\" class='visualizationMainDivwrapper width60'>"
					+ "<div class=\"visualizationHeaderDiv\" onclick=\"visualizationDivToggle()\">"
					+ "<span class=\"visualizationtitle\">" + "<h4>Visualizations</h4>" + "</span>"
					+ "<span class=\"toggleImg\" id=\"visualToggleIcon\"><img src=\"images/toggle_blueIcon.png\" width=\"16px;\"></span>"
					+ "</div>" + "<div id =\"Visualization\" class='VisionAnalyticsBICharts visualBIChart'>"
					+ "<div id=\"VisionBIVisualization\">"
					+ "<div id = 'visionVisualizeSlicerId' class='visionVisualizeSlicerClass'>"
					+ "<div class='visionVisualizeSlicerImageDivClass'><img src=\"images/Chart_Slicer.svg\" onclick=\"showSlicerField('visionVisualizeSlicerFieldId')\" width=\"20px\" id=\"VisionVisualizationSlicerImageId\" class=\"VisionVisualizationSlicerImageClass\" title=\"Click for Slicer\"/></div>"
					+ "<div id ='visionVisualizeSlicerFieldId' class='visionVisualizeSlicerFieldClass' style='display:none'><span>Drop Fields Here</span></div>"
					+ "</div>" + "<div id='visualizeConfigTabs' class='visualizeConfigTabsClass'>"
					+ "<ul id='visionVisualizeConfig'>"
					+ "<li id='visionVisualizeFields' class='visionVisualizeFieldsClass'><img src='images/Fields_Selection.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigColumns','visionVisualizeFields')\"/></li>"
					+ "<li id='visionVisualizeConfiguration' class='visionVisualizeConfigurationClass'><img src='images/Chart_Config.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigProperties','visionVisualizeConfiguration')\"/></li>"
					+ "<li id='visionVisualizeFilters' class='visionVisualizeFiltersClass'><img src='images/Filter.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigFilters','visionVisualizeFilters')\"/></li>"
					+ "<li id='visionVisualizeJoins' class='visionVisualizeJoinsClass'><img src='images/mapping.svg' style='cursor:pointer;' onclick=\"showChartConfigurationDIv('visualizeChartConfigJoins','visionVisualizeJoins')\"/></li>"
					+ "</ul>" + "</div>"
					+ "<div id=\"visualizeChartConfigColumns\" class=\"visualizeChartConfigColumnsClass\"></div>"
					+ "<div id=\"visualizeChartConfigProperties\" class=\"visualizeChartConfigPropertiesClass\" style='display:none'></div>"
					+ "<div id=\"visualizeChartConfigFilters\" class=\"visualizeChartConfigFiltersClass\" style='display:none'></div>"
					+ "<div id=\"visualizeChartConfigJoins\" class=\"visualizeChartConfigJoinsClass\" style='display:none'></div>"
					// + "</div>"
					+ "</div>" + "</div>" + "</div>" + "<div class=\"chartView\" id=\"visualizeArea\">"
					+ "<div class=\"visionVisualizationDataChartcount\" id=\"visionVisualizationDataChartcount\">"
					+ "<div class=\"visionVisualizationDataChartViewCLass\" id=\"visionVisualizationDataChartViewId\">"
					+ "</div>" + "</div>" + "</div>";
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public JSONObject getconfigobject(HttpServletRequest request) {
		return dashBoardsDAO.getconfigobject(request);
	}

	public JSONObject getCurrentDBTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();

		try {

			resultObj = dashBoardsDAO.getCurrentDBTables(request);

		} catch (Exception e) {
		}
		return resultObj;
	}

	public JSONObject getChartFilterData(HttpServletRequest request) {
		return dashBoardsDAO.getChartFilterData(request);
	}

	public JSONObject getHomeChartSlicerData(HttpServletRequest request) {
		return dashBoardsDAO.getHomeChartSlicerData(request);
	}

	public JSONObject fetchHomeSlicerValues(HttpServletRequest request) {
		return dashBoardsDAO.fetchHomeSlicerValues(request);
	}

	public JSONObject getSlicerHomeCharts(HttpServletRequest request) {
		return dashBoardsDAO.getSlicerHomeCharts(request);
	}

	public JSONObject movingAvgData(HttpServletRequest request) {
		List selectData = null;
		JSONObject chartObj = new JSONObject();
		List<String> columnKeys = new ArrayList<>();
		String chartConfigObjStr = "";
		JSONObject filteredChartConfigObj = new JSONObject();
		JSONObject chartConfigObj = new JSONObject();
		try {
			String chartType = request.getParameter("chartType");
			String chartId = request.getParameter("chartId");
			String chartConfigPositionKeyStr = request.getParameter("chartConfigPositionKeyStr");
			JSONObject chartListObj = dashBoardsDAO.movingAvgData(request);
			if (chartListObj != null && !chartListObj.isEmpty()) {
				selectData = (List) chartListObj.get("chartList");
				columnKeys = (List<String>) chartListObj.get("columnKeys");

			}
			JSONObject dataObject = dashBoardsDAO.getVisualizationData(request, chartId);
			chartConfigObjStr = (String) dataObject.get("chartPropObj");
			chartConfigPositionKeyStr = (String) dataObject.get("chartConfigObj");
			chartConfigPositionKeyStr = (String) dataObject.get("chartConfigObj");
			chartConfigPositionKeyStr = (String) dataObject.get("chartConfigObj");
			if (chartConfigObjStr != null && !"".equalsIgnoreCase(chartConfigObjStr)
					&& !"null".equalsIgnoreCase(chartConfigObjStr)) {
				chartConfigObj = (JSONObject) JSONValue.parse(chartConfigObjStr);
			}

			for (Object chartKey : chartConfigObj.keySet()) {
				String key = String.valueOf(chartKey);
				String filteredKey = key.replaceAll("\\d", "");
				filteredChartConfigObj.put(filteredKey, chartConfigObj.get(key));
			}
			JSONObject configObj = dashBoardsDAO.buildOptionsObj(request, filteredChartConfigObj,
					chartConfigPositionKeyStr, chartId, chartType);
			JSONObject layoutObj = (JSONObject) configObj.get("layoutObj");
			JSONObject dataPropObj = (JSONObject) configObj.get("dataObj");
			JSONObject framedChartDataObj = dashBoardsDAO.getFramedMovingAvgDataObject(selectData, columnKeys,
					layoutObj, dataPropObj);
			if (framedChartDataObj != null && !framedChartDataObj.isEmpty()) {
				chartObj.put("layout", (JSONObject) framedChartDataObj.get("layoutObj"));
				if (chartType != null && !"".equalsIgnoreCase(chartType) && "treemap".equalsIgnoreCase(chartType)) {
					JSONObject treeMapDataObj = dashBoardsDAO.getTreeMapDataObject(framedChartDataObj, columnKeys);
					if (treeMapDataObj != null && !treeMapDataObj.isEmpty()) {
						chartObj.put("treeMapCol", treeMapDataObj.get("treeMapColObj"));
						chartObj.put("data", treeMapDataObj.get("data"));
					}
				} else {
					chartObj.put("data", (JSONObject) framedChartDataObj.get("dataObj"));
				}
			}
			chartObj.put("dataPropObject", dataPropObj);
			chartObj.put("columnObj", chartListObj.get("columnObj"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return chartObj;

	}

	public String insertdata(HttpServletRequest request) {
		return dashBoardsDAO.insertdata(request);
	}

	public JSONObject getlandingGraphData(HttpServletRequest request) {
		return dashBoardsDAO.getlandingGraphData(request);
	}

	public String getdashbordname(HttpServletRequest request) {
		return dashBoardsDAO.getdashbordname(request);
	}

	public JSONObject getJqxPivotGridData(String gridId, HttpServletRequest request) {
		return this.dashBoardsDAO.getJqxPivotGridData(gridId, request);
	}

	public JSONObject getPivotGridData(String gridId, HttpServletRequest request) {
		return this.dashBoardsDAO.getPivotGridData(gridId, request);
	}

	public String updatechartSettingdata(HttpServletRequest request) {
		return dashBoardsDAO.updatechartSettingdata(request);
	}

	public JSONObject getSchemaObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getObjectdata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public String gridUpdateRecords(HttpServletRequest request, JSONObject newGridData, String baseTableName,
			String gridId) {
		String resultobj = "";
		try {
			resultobj = dashBoardsDAO.gridUpdateRecords(request, newGridData, baseTableName, gridId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;

	}

	public JSONObject saveHomeChartsColorsData(HttpServletRequest request) {
		return dashBoardsDAO.saveHomeChartsColorsData(request);
	}

	public JSONObject getSurveyHomeCharts(HttpServletRequest request) {
		return dashBoardsDAO.getSurveyHomeCharts(request);
	}

	public String updteFilterColumn(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.updteFilterColumn(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String saveChartRadioButtonColumns(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.saveChartRadioButtonColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String updteCompareFilterColumn(HttpServletRequest request) {
		return dashBoardsDAO.updteCompareFilterColumn(request);

	}

	public JSONObject getHomeChartHeaderFilterForm(HttpServletRequest request) {
		return dashBoardsDAO.getHomeChartHeaderFilterForm(request);
	}

	public JSONObject updteCompareFilterColumnsData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		String result = "";
		try {
			JSONObject compareFilterDataObj = new JSONObject();
			String compareFilterData = request.getParameter("compareFilterData");
			String dashBoardName = request.getParameter("dashbordName");
			if (compareFilterData != null && !"".equalsIgnoreCase(compareFilterData)
					&& !"".equalsIgnoreCase(compareFilterData)) {
				compareFilterDataObj = (JSONObject) JSONValue.parse(compareFilterData);
			}
			JSONArray dataColArr = new JSONArray();
			JSONObject dataColObj = new JSONObject();
			result = "<div id='dashBoardHomeCompareFiltersId' class=\"dashBoardHomeCompareFiltersClass row\">";
			if (compareFilterDataObj != null && !compareFilterDataObj.isEmpty()) {
				String chartCount = "";
				result += "<div id='dashBoardHomeChartOneCompareFiltersId' class='dashBoardHomeChartOneCompareFiltersClass col-md-6 col-sm-6 col-lg-6'>";
				result += "<p class='dashBoardHomeChartOneCompareFiltersTableSpanClass'>Chart 1:</p>";
				JSONArray compareOneFiltersArr = (JSONArray) compareFilterDataObj.get("chart1");
				result += "<table id='dashBoardHomeChartOneCompareFiltersTableId' class= 'dashBoardHomeChartOneCompareFiltersTableClass'>";
				for (int i = 0; i < compareOneFiltersArr.size(); i++) {
					String columnVal = (String) compareOneFiltersArr.get(i);
					chartCount = "one";
					result += "<tr class='visionDashBoardCompareChartFiltersTrClass'>";
					if (columnVal != null && !"".equalsIgnoreCase(columnVal)) {
						String colName = columnVal.split("\\.")[1];
						if (colName != null && !"".equalsIgnoreCase(colName) && colName.contains("DATE")) {
							JSONObject dateNormal = new JSONObject();
							dateNormal.put("tbid", "tbone" + i);
							dateNormal.put("type", "normal");
							dataColArr.add(dateNormal);
							JSONObject dateMin = new JSONObject();
							dateMin.put("tbid", "tbminone" + i);
							dateMin.put("type", "min");
							dataColArr.add(dateMin);
							JSONObject dateMax = new JSONObject();
							dateMax.put("tbid", "tbmaxone" + i);
							dateMax.put("type", "max");
							dataColArr.add(dateMax);
							result += "<td id='td" + i + "' data-columnName='" + columnVal
									+ "' data-Range='Y' class='visionDashBoardCompareChartFiltersTdClass'>";
						} else {
							result += "<td id='td" + i + "' data-columnName='" + columnVal
									+ "' data-Range='N' class='visionDashBoardCompareChartFiltersTdClass'>";
						}

						String values = dashBoardsDAO.getSurveyAnalyticPartyWiseFilters(columnVal.split("\\.")[0],
								columnVal.split("\\.")[1], i, chartCount);
						if (values != null && !"".equalsIgnoreCase(values)) {
							JSONObject valueObj = (JSONObject) JSONValue.parse(values);
							if (valueObj != null && !valueObj.isEmpty()) {
								result += (String) valueObj.get("result");
								if (colName != null && !"".equalsIgnoreCase(colName) && !colName.contains("DATE")) {
									dataColObj.put("tbValues" + chartCount + i, valueObj.get("checkBoxDataArr"));
								}
							}
						}
						result += "</td>";
						result += "</tr>";
					}
				}
				result += "</table>";
				result += "</div>";
				result += "<div id='dashBoardHomeChartTwoCompareFiltersId' class='dashBoardHomeChartTwoCompareFiltersClass col-md-6 col-sm-6 col-lg-6'>";
				JSONArray compareTwoFiltersArr = (JSONArray) compareFilterDataObj.get("chart2");
				result += "<p class='dashBoardHomeChartTwoCompareFiltersTableSpanClass'>Chart 2:</p>";
				result += "<table id='dashBoardHomeChartTwoCompareFiltersTableId' class= 'dashBoardHomeChartTwoCompareFiltersTableClass'>";
				for (int i = 0; i < compareTwoFiltersArr.size(); i++) {
					chartCount = "two";
					result += "<tr class='visionDashBoardCompareChartFiltersTrClass'>";
					String columnVal = (String) compareTwoFiltersArr.get(i);
					if (columnVal != null && !"".equalsIgnoreCase(columnVal)) {
						String colName = columnVal.split("\\.")[1];
						if (colName != null && !"".equalsIgnoreCase(colName) && colName.contains("DATE")) {
							JSONObject dateNormal = new JSONObject();
							dateNormal.put("tbid", "tbtwo" + i);
							dateNormal.put("type", "normal");
							dataColArr.add(dateNormal);
							JSONObject dateMin = new JSONObject();
							dateMin.put("tbid", "tbmintwo" + i);
							dateMin.put("type", "min");
							dataColArr.add(dateMin);
							JSONObject dateMax = new JSONObject();
							dateMax.put("tbid", "tbmaxtwo" + i);
							dateMax.put("type", "max");
							dataColArr.add(dateMax);
							result += "<td id='td" + i + "' data-columnName='" + columnVal
									+ "' data-Range='Y' class='visionDashBoardCompareChartFiltersTdClass'>";
						} else {
							result += "<td id='td" + i + "' data-columnName='" + columnVal
									+ "' data-Range='N' class='visionDashBoardCompareChartFiltersTdClass'>";
						}

						String values = dashBoardsDAO.getSurveyAnalyticPartyWiseFilters(columnVal.split("\\.")[0],
								columnVal.split("\\.")[1], i, chartCount);
						if (values != null && !"".equalsIgnoreCase(values)) {
							JSONObject valueObj = (JSONObject) JSONValue.parse(values);
							if (valueObj != null && !valueObj.isEmpty()) {
								result += (String) valueObj.get("result");
								if (colName != null && !"".equalsIgnoreCase(colName) && !colName.contains("DATE")) {
									dataColObj.put("tbValues" + chartCount + i, valueObj.get("checkBoxDataArr"));
								}
							}
						}
						result += "</td>";
						result += "</tr>";
					}
				}

				result += "</table>";
				result += "</div>";
			}
			result += "</div>";
			result += "<div class ='visionDbCompareChartsFilterButtonDivClass'><button type='button' class='visionDbCompareChartsFilterButton btn btn-primary' value='Apply' onclick=\"applyCompareChartFilters('"
					+ dashBoardName + "')\">Apply</button></div>";
			resultObj.put("result", result);
			resultObj.put("jsDateItems", dataColArr);
			resultObj.put("dataColObj", dataColObj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject createFilterHeader(HttpServletRequest request) {
		return dashBoardsDAO.createFilterHeader(request);
	}

	public JSONObject getDrillDownFilterColumns(HttpServletRequest request) {
		String Resultstr = "";
		JSONObject resultobj = new JSONObject();
//        String tablename = request.getParameter("table");
		try {
			String tableName = dashBoardsDAO.getChartTable(request);
			String result = dashBoardsDAO.getLoadTableColumns(request, tableName);
			JSONObject chartConfigObj = getChartFilters(request);
			resultobj.put("chartConfigObj", chartConfigObj);
			JSONObject dropDownObj = new JSONObject();
			dropDownObj.put("chartConfigObj", chartConfigObj.toString());
			Resultstr = "<div id='FilterColumndataId' class = 'FilterColumndataClass'>"
					+ "<div id=\"VisualizeBIFilterColumns\"></div>"
					+ "<div id=\"visualizeChartConfigFiltersData\" class=\"visualizeChartConfigFiltersClass\"></div>"
					+ "</div>";
			String chartTypes = "<select id='drillDownChartTypeId' class='drillDownChartTypeClass' onchange=getDrillDownConfigByChartType()>"
					+ "<option value='pie'>Pie</option>" + "<option value='donut'>Donut</option>"
					+ "<option value='bar'>Bar</option>" + "<option value='column'>Column</option>"
					+ "<option value='lines'>Line</option>" + "</select>";
			resultobj.put("Resultstr", Resultstr);
			resultobj.put("result", result);
			resultobj.put("tableName", tableName);
			resultobj.put("chartTypes", chartTypes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;

	}

	public void downloadALLChartsInPDF(HttpServletRequest request, HttpServletResponse response) {
		OutputStream os = null;
		com.itextpdf.text.Document document = null;
		try {
			String filename = "ALLCharts.pdf";
			response.reset();
			File filelocation = new File("C:/tempfiles");
			if (!filelocation.exists()) {
				filelocation.mkdir();
			}
			String filepath = filelocation.getAbsolutePath() + File.separator + filename;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			response.setContentType("application/pdf");
			response.setCharacterEncoding("UTF-8");
			os = response.getOutputStream();
			document = new com.itextpdf.text.Document(PageSize.A4, 0, 0, 0, 0);
			Rectangle pageSize = new Rectangle(PageSize.A4);
			PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filepath));
			document.open();
			HttpSession session = request.getSession(false);
			String imgObjStr = request.getParameter("chartImageObj");
			if (imgObjStr != null && !imgObjStr.isEmpty()) {
				JSONObject imgObj = (JSONObject) JSONValue.parse(imgObjStr);
				if (imgObj != null && !imgObj.isEmpty()) {
					int key = 1;
					int x = 0;
					int y = 550;
					int x1 = 0;
					int y1 = 550;
					for (int i = 1; i <= imgObj.size() + 1; i++) {
						String image = (String) imgObj.get(String.valueOf(i));
						if (image != null && i <= 9) {
							Image img = getImage(image);
							float width = img.getWidth();
							if (width > 1000) {
								img.scaleAbsoluteWidth(600f);
							} else {
								img.scaleAbsoluteWidth(180f);
//								img.scaleAbsoluteWidth(220f);
							}
							img.scaleAbsoluteHeight(220f);
							img.setWidthPercentage(100);
							img.setAbsolutePosition(x, y);
							document.add(img);
							if (width > 1000) {
								if (key % i == 0) {
									y = y - 250;
									x = 0;
								}

							} else {
								x = x + 200;
								if (key % 3 == 0) {
									y = y - 250;
									x = 0;
								}
							}
							key++;
						}
						if (image != null && i >= 10 && i <= 18) {
							if (i == 10) {
								document.newPage();
							}
							Image img = getImage(image);
							img.scaleAbsoluteWidth(180f);
//							img.scaleAbsoluteWidth(220f);
							img.scaleAbsoluteHeight(220f);
							img.setWidthPercentage(100);
							img.setAbsolutePosition(x1, y1);
							document.add(img);
							x1 = x1 + 200;
							if (key % 3 == 0) {
								y1 = y1 - 250;
								x1 = 0;
							}
							key++;
						}
						if (image != null && i >= 19 && i <= 27) {
							if (i == 19) {
								document.newPage();
							}
							Image img = getImage(image);
							img.scaleAbsoluteWidth(180f);
//							img.scaleAbsoluteWidth(220f);
							img.scaleAbsoluteHeight(220f);
							img.setWidthPercentage(100);
							img.setAbsolutePosition(x1, y1);
							document.add(img);
							x1 = x1 + 200;
							if (key % 3 == 0) {
								y1 = y1 - 250;
								x1 = 0;
							}
							key++;
						}
					}
					document.close();
					InputStream fis = new FileInputStream(new File(filepath));
					String mimeType = request.getServletContext().getMimeType(filename);
					response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
					response.setHeader("Content-disposition", "attachment; filename=\"" + filename + "\"");
					byte[] bufferData = new byte[1024];
					int read = 0;
					while ((read = fis.read(bufferData)) != -1) {
						os.write(bufferData, 0, read);
					}
					os.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (document != null && document.isOpen()) {
					document.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public Image getImage(String src) throws DocumentException {
		int pos = src.indexOf("base64,");
		Image image = null;
		try {
			if (src.startsWith("data") && pos > 0) {
				byte[] img = Base64.decode(src.substring(pos + 7).getBytes());
				image = Image.getInstance(img);

			} else {
				image = Image.getInstance(src);
			}
		} catch (IOException ex) {
			System.out.println("BadElementException :: " + ex.getMessage());
		}
		return image;
	}

	public JSONObject getChartHomePageDiv(HttpServletRequest request) {
		JSONObject homePageDivObj = new JSONObject();
		try {
			
			String divStr = "<div id=\"mainintelliSenseSelectBoxId\" class='intelliSenseSelectBoxClass' onclick=\"scrollerWheel()\">"
					+ "<div class=\"iconMenuNavPrev\"><i class=\"fa fa fa-angle-double-left\"></i></div>"
					+ "<div id='mainintelliSenseInnerSelectBoxId'class='intelliSenseSelectBoxClass'></div>"
					+ "<div class=\"chartSelectionsDropDown\" style=\"display:none\">"
					+ "<div id=\"OptionListData\" class='visionVisualizeHomePageDropdown'>" + "</div>"
					+ "<div id=\"visionHomePageSlicer\" class=\"visionHomePageSlicerClass\"></div>"
					+ "<div id=\"visionFilterData\" class=\"visionFilterData\"></div>"
					+ "<div id=\"visionHomeKanbanView\" class=\"visionHomeKanbanViewClass\"></div>"
					+ "<div id=\"visionChartColorPalleteId\" class=\"visionChartColorPalleteClass\"></div>" 
					+ "</div>"
					+ "<div class=\"expendInOutDivClass\" id=\"expendInOutDivID\" onclick=\"shrinkExpandCard()\"><i class=\"fa fa-angle-double-up\" aria-hidden=\"true\"></i></div>"
					+ "<div id=\'visionDashBoardHomeFilterId\' class=\'visionDashBoardHomeFilterClass\' style=\"display:none\">" 
					+"</div>" 
					+ "<div class=\"iconMenuNavNext\"><i class=\"fa fa fa-angle-double-right\"></i></div>"
					+ "</div>"
					
					+ "<section class=\"visualizationDashboardView\" style=\"display:none\">"
					+ "<div class=\"container-fluid\">" + "<div class=\"row\">" 
					+ " <div id='upperCpmpaireMainDIvID' class=\"col-12\">" 
																
					+ "<div id='visionDashBoardHomeCompareFilterId' class='visionDashBoardHomeFilterClass row' style=\"display:none\"></div>"
					+ "<div id=\"visionCardView\" class=\"visionCardViewClass\">" + "</div>" + "</div>" + "</div>"
					+ "<div class=\"container-fluid\" id ='visualizecharts'>"
					+ "<div id=\"visualizechartId\" class='visionVisualizeHomePageCharts row'></div>" + "</div> "
					+ "</div>" + "</section>"
					+ "<form action='downloadChartImageAllPDF' id='pdfChartForm'  method='POST' target='_blank' >\n"
					+ "<c:if test=\"true\">\n"
					+ "<input type=\"hidden\" name=\"${_csrf.parameterName}\" value=\"${_csrf.token}\" /> \n"
					+ "</c:if> \n" + "<input type=\"hidden\" value=\"\" id=\"chartImageObj\" name=\"chartImageObj\"/>\n"
					+ "</form>" + "<div id=\"dialog\"></div>" + "<div id=\"dialog1\"></div>"
					+ "<div id=\"gridDialog\"></div>" + "<div id=\"homepageChartDialog\"></div>"
					+ "<div id ='drillDownChartDataDialog'></div>" + "<div id ='exchangeTreeDialog'></div>"
					+ "<div id ='dxpCreatePopOver'></div>" + "<div id ='smartBiTreeDateCalendarPopup'>"
					
					+ "</div>";
			
			/*
			 * String divStr =
			 * "<div id=\"mainintelliSenseSelectBoxId\" class='intelliSenseSelectBoxClass'>"
			 * +
			 * "<div id='mainintelliSenseInnerSelectBoxId'class='intelliSenseSelectBoxClass'></div>"
			 * + "<div class=\"chartSelectionsDropDown\" style=\"display:none\">" +
			 * "<div id=\"OptionListData\" class='visionVisualizeHomePageDropdown'>" +
			 * "</div>" +
			 * "<div id=\"visionHomePageSlicer\" class=\"visionHomePageSlicerClass\"></div>"
			 * + "<div id=\"visionFilterData\" class=\"visionFilterData\"></div>" +
			 * "<div id=\"visionHomeKanbanView\" class=\"visionHomeKanbanViewClass\"></div>"
			 * +
			 * "<div id=\"visionChartColorPalleteId\" class=\"visionChartColorPalleteClass\"></div>"
			 * + "</div>" + "</div>" +
			 * "<section class=\"visualizationDashboardView\" style=\"display:none\">" +
			 * "<div class=\"container-fluid\">" + "<div class=\"row\">" +
			 * " <div class=\"col-12\">" +
			 * "<div id='visionDashBoardHomeFilterId' class='visionDashBoardHomeFilterClass row' style=\"display:none\"></div>"
			 * +
			 * "<div id='visionDashBoardHomeCompareFilterId' class='visionDashBoardHomeFilterClass row' style=\"display:none\"></div>"
			 * + "<div id=\"visionCardView\" class=\"visionCardViewClass\">" + "</div>" +
			 * "</div>" + "</div>" + "<div class=\"container-fluid\" id ='visualizecharts'>"
			 * +
			 * "<div id=\"visualizechartId\" class='visionVisualizeHomePageCharts row'></div>"
			 * + "</div> " + "</div>" + "</section>" +
			 * "<form action='downloadChartImageAllPDF' id='pdfChartForm'  method='POST' target='_blank' >\n"
			 * + "<c:if test=\"true\">\n" +
			 * "<input type=\"hidden\" name=\"${_csrf.parameterName}\" value=\"${_csrf.token}\" /> \n"
			 * + "</c:if> \n" +
			 * "<input type=\"hidden\" value=\"\" id=\"chartImageObj\" name=\"chartImageObj\"/>\n"
			 * + "</form>" + "<div id=\"dialog\"></div>" + "<div id=\"dialog1\"></div>" +
			 * "<div id=\"gridDialog\"></div>" + "<div id=\"homepageChartDialog\"></div>" +
			 * "<div id ='drillDownChartDataDialog'></div>" +
			 * "<div id ='exchangeTreeDialog'></div>" + "<div id ='dxpCreatePopOver'></div>"
			 * + "<div id ='smartBiTreeDateCalendarPopup'></div>";
			 */
			homePageDivObj.put("chartDiv", divStr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return homePageDivObj;
	}

	public JSONObject showDrillDownChart(HttpServletRequest request) {
		return dashBoardsDAO.showDrillDownChart(request);
	}

	public JSONObject getcolorpalleteform(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		String result = "";
		JSONObject markerobj = new JSONObject();
		try {
			String data = request.getParameter("coloobjdata");
			String chartid = request.getParameter("chartid");
			String defaultColors = request.getParameter("defaultColors");
			JSONArray defaultValuesArr = new JSONArray();
			if (defaultColors != null && !"".equalsIgnoreCase(defaultColors)) {
				defaultValuesArr = (JSONArray) JSONValue.parse(defaultColors);
			}
			String defaultClrStr = "";
			if (defaultValuesArr != null && !defaultValuesArr.isEmpty()) {
				for (int c = 0; c < defaultValuesArr.size(); c++) {
					defaultClrStr += "<span class='themeBtns' data-color='" + defaultValuesArr.get(c)
							+ "' style='background-color: " + defaultValuesArr.get(c) + ";'></span>";
					if (c == 5) {
						break;
					}
				}
			}
			JSONArray dataarray = new JSONArray();
			if (data != null && !"".equalsIgnoreCase(data) && !"null".equalsIgnoreCase(data)) {  
				dataarray = (JSONArray) JSONValue.parse(data);
				markerobj = (JSONObject) dataarray.get(0); 
			}
			result += " <div class='colorPallatteMainDiv'>";
			if (chartid != null && !"".equalsIgnoreCase(chartid)) {
				result += "<div class='themeBtnsContainer'>"
						+ " <div class='colorblockTitle'><h5>Default Color</h5></div>"
						+ "<div class='colorPalletteSection'>"
						+ "<div class='themeBtnsGroup' onclick=saveGrpahColors(event,'" + chartid
						+ "') tittle=''Click to Save Color onmouseover=\"updatecolorOnGraph(event,'" + chartid + "')\">"
						+ defaultClrStr + "</div>" + "  </div>" + " </div>";
			}

			result += "<div class='themeBtnsContainer'>" + " <div class='colorblockTitle'><h5>Palette</h5></div>"
					+ "<div class='colorPalletteSection'>"
					+ "<div class='themeBtnsGroup' onclick=saveGrpahColors(event,'" + chartid
					+ "') tittle=''Click to Save Color onmouseover=\"updatecolorOnGraph(event,'" + chartid + "')\">"
					+ "<div>"
					+ "<span class='themeBtns' data-color='#696969' style='background-color: #696969;'></span>"
					+ "  <span class='themeBtns' data-color='#888888' style='background-color: #888888;'></span>"
					+ " <span class='themeBtns' data-color='#A0A0A0' style='background-color: #A0A0A0;'></span>"
					+ "</div>" + "<div>"
					+ "  <span class='themeBtns' data-color='#A8A8A8' style='background-color: #A8A8A8;'></span>"
					+ "  <span class='themeBtns' data-color='#B8B8B8' style='background-color: #B8B8B8;'></span>"
					+ "  <span class='themeBtns' data-color='#C0C0C0' style='background-color: #C0C0C0;'></span>"
					+ "</div>" + "  </div>" + "<div class='themeBtnsGroup' onclick=saveGrpahColors(event,'" + chartid
					+ "') tittle=''Click to Save Color onmouseover=\"updatecolorOnGraph(event,'" + chartid + "')\">"
					+ "<div>"
					+ "<span class='themeBtns' data-color='#00acee' style='background-color: #00acee;'></span>"
					+ "  <span class='themeBtns' data-color='#00b9ff' style='background-color: #00b9ff;'></span>"
					+ " <span class='themeBtns' data-color='#2bc4ff' style='background-color: #2bc4ff;'></span>"
					+ "</div>" + "<div>"
					+ "  <span class='themeBtns' data-color='#00aaee' style='background-color: #00aaee;'></span>"
					+ "  <span class='themeBtns' data-color='#26a7de' style='background-color: #26a7de;'></span>"
					+ "  <span class='themeBtns' data-color='#45b1e8' style='background-color: #45b1e8;'></span>"
					+ "</div>" + "  </div>" + "<div class='themeBtnsGroup' onclick=saveGrpahColors(event,'" + chartid
					+ "') tittle=''Click to Save Color onmouseover=\"updatecolorOnGraph(event, '" + chartid + "')\">"
					+ "<div>"
					+ "<span class='themeBtns' data-color='#006400' style='background-color: #006400;'></span>"
					+ "  <span class='themeBtns' data-color='#008000' style='background-color: #008000;'></span>"
					+ " <span class='themeBtns' data-color='#228B22' style='background-color: #228B22;'></span>"
					+ "</div>" + "<div>"
					+ "  <span class='themeBtns' data-color='#347C2C' style='background-color: #347C2C;'></span>"
					+ "  <span class='themeBtns' data-color='#437C17' style='background-color: #437C17;'></span>"
					+ "  <span class='themeBtns' data-color='#4AA02C' style='background-color: #4AA02C;'></span>"
					+ "</div>" + "  </div>" + "<div class='themeBtnsGroup' onclick=saveGrpahColors(event,'" + chartid
					+ "') tittle=''Click to Save Color onmouseover=\"updatecolorOnGraph(event,'" + chartid + "')\">"
					+ "<div>"
					+ "<span class='themeBtns' data-color='#EAC117' style='background-color: #EAC117;'></span>"
					+ "  <span class='themeBtns' data-color='#806517' style='background-color: #806517;'></span>"
					+ " <span class='themeBtns' data-color='#5C3317' style='background-color: #5C3317;'></span>"
					+ "</div>" + "<div>"
					+ "  <span class='themeBtns' data-color='#347C2C' style='background-color: #347C2C;'></span>"
					+ "  <span class='themeBtns' data-color='#E66C2C' style='background-color: #E66C2C;'></span>"
					+ "  <span class='themeBtns' data-color='#C11B17' style='background-color: #C11B17;'></span>"
					+ "</div>" + "  </div>" + "<div class='themeBtnsGroup' onclick=saveGrpahColors(event,'" + chartid
					+ "') tittle=''Click to Save Color onmouseover=\"updatecolorOnGraph(event,'" + chartid + "')\">"
					+ "<div>"
					+ "<span class='themeBtns' data-color='#00008B' style='background-color: #00008B;'></span>"
					+ "  <span class='themeBtns' data-color='#191970' style='background-color: #191970;'></span>"
					+ " <span class='themeBtns' data-color='#000080' style='background-color:#000080;'></span>"
					+ "</div>" + "<div>"
					+ "  <span class='themeBtns' data-color='#0000A0' style='background-color: #0000A0;'></span>"
					+ "  <span class='themeBtns' data-color='#0020C2' style='background-color: #0020C2;'></span>"
					+ "  <span class='themeBtns' data-color='#0909FF' style='background-color: #0909FF;'></span>"
					+ "  </div>" + "</div>" + "<div class='themeBtnsGroup' onclick=saveGrpahColors(event,'" + chartid
					+ "') tittle=''Click to Save Color onmouseover=\"updatecolorOnGraph(event,'" + chartid + "')\">"
					+ "<div>"
					+ "<span class='themeBtns' data-color='#00acee' style='background-color: #00acee;'></span>"
					+ "  <span class='themeBtns' data-color='#5cb9f1' style='background-color: #5cb9f1;'></span>"
					+ " <span class='themeBtns' data-color='#86c7f4' style='background-color: #86c7f4;'></span>"
					+ "</div>" + "<div>"
					+ "  <span class='themeBtns' data-color='#a8d5f7' style='background-color: #a8d5f7;'></span>"
					+ "  <span class='themeBtns' data-color='#c6e3fa' style='background-color: #c6e3fa;'></span>"
					+ "  <span class='themeBtns' data-color='#e3f1fc' style='background-color: #e3f1fc;'></span>"
					+ "</div>" + "  </div>" + " </div>" + " </div>" + "</div>";

			resultobj.put("colorpalateobj", result);
			resultobj.put("data", dataarray);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	public JSONObject getChartColumnsForm(HttpServletRequest request) {
		return dashBoardsDAO.getChartColumnsForm(request);
	}

	public JSONObject getchartconfigobjdata(HttpServletRequest request) {
		JSONObject dataobj = new JSONObject();
		try {
			dataobj = dashBoardsDAO.getchartPropertiesobj(request);
			if (dataobj != null && !dataobj.isEmpty()) {

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataobj;
	}

	public JSONObject updateGraphProperties(HttpServletRequest request) {
		JSONObject dataobj = new JSONObject();
		try {
			dataobj = dashBoardsDAO.updateGraphProperties(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataobj;
	}

	public JSONObject getTreeMapExchangeLevels(HttpServletRequest request) {
		JSONObject dataobj = new JSONObject();
		try {
			dataobj = dashBoardsDAO.getTreeMapExchangeLevels(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataobj;
	}

	public JSONObject getExchaneLevelsData(HttpServletRequest request) {
		return dashBoardsDAO.getExchaneLevelsData(request);
	}

	public JSONObject createTableasFile(HttpServletRequest request, HttpServletResponse response) {
		JSONObject resultObj = new JSONObject();
		String buttonDiv = "";
		String ListBoxId = "";
		JSONArray checkBoxDataArr = new JSONArray();
		try {
			resultObj = dashBoardsDAO.createTableasFile(request, response);
			String tableName = request.getParameter("tableName");
			String filePath = request.getParameter("filePath");
			if (tableName != null && !tableName.isEmpty()) {
				String ClolumnListStr = "";
				try {
					List ColumnList = dashBoardsDAO.tableColumnList(request, tableName);
//                ListBoxId = "<div id = 'tablecolumnId' class = 'tablecolumnId'>";
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

			if (resultObj != null) {                  
//              buttonDiv="<span><img src='images/split.png' width='16px' title='split data' onclick= splitData('"+tableName+"')>";
				buttonDiv = "<span><img src='images/Delete-Icon-03-01.png' title='Delete' onclick=deleteColumn("
						+ checkBoxDataArr + ",'" + tableName + "')>";
				buttonDiv += "<img src='images/Data Merge-Icon-01.png' title='Merge Data' onclick= mergeColumntwthData('" + tableName
						+ "')>";
				buttonDiv += "<img src='images/Data Transpose-Icon-01-01.png' title='Transpose Data' onclick= composeData(event,'"
						+ tableName + "')>";
				buttonDiv += "<img src='images/Dimention-Transpsose-Icon-02-01.png' title='Dimensional Transpose' onclick= DimensionTranspose(event,'"
						+ tableName + "')>";
				buttonDiv += "<img src='images/Change Datatype-Icon-03-01.png' title='Table Edit' onclick= ChooseOptions(event,'"
						+ tableName + "')>";
				//buttonDiv += "<img src='images/Export-Icon-03-01.png' title='Export'id ='ExportgridId' onclick= generateexcel>";
				buttonDiv += "<img src='images/Chart Auto-Suggetion-Icon-03-01.png' title='AI Chart Suggestions' onclick= getModalFileColumns(event,'"
						+ tableName + "')>";
				buttonDiv += "<img src='images/Pivot Descriptor-Icon-03-01.png' title='Pivot Table' onclick= getCrossTabData('" + tableName
						+ "')>";
				/*
				 * buttonDiv +=
				 * "<img src='images/Pivot table-Icon-03-01.png' title='Pivot Table' onclick= getPivotGridData('"         
				 * + tableName + "')>"; buttonDiv +=
				 * "<img src='images/Pivot table-Icon-03-01.png' title='Data Correlation' onclick= getDataCorrelation('"
				 * + filePath + "')>";
				 */
				buttonDiv += "</span>";   

			}
//            buttonDiv +="<div id = 'tablecolumnId' class = 'tablecolumnId'>";

			resultObj.put("buttons", buttonDiv);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject deleteTableColumn(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.deleteTableColumn(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject mergeformdata(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsDAO.mergeformdata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	public String transformdata(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.transformdata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String gettransposedata(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.gettransposedata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public String DimensionTransposeColumn(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		String Result = "";
		try {
			Result = dashBoardsDAO.DimensionTransposeColumn(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}

	public boolean setImportData(HttpServletRequest request) {
		boolean result = false;
		try {
			result = dashBoardsDAO.setImportData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public JSONObject showtableData(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsDAO.showtableData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	public JSONObject gettableObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.showtableData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public String gettableattribute(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		String Result = "";
		try {
			Result = dashBoardsDAO.gettableattribute(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}

	public String caseSensitive(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.caseSensitive(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public JSONObject DimensionTransposedata(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsDAO.DimensionTransposedata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	public JSONObject generateQueryStr(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		String tableStr = "";
		String ColumnStr = "";
		try {
			List tablelist = dashBoardsDAO.gettableList(request);
			List columnlist = dashBoardsDAO.gettablecolumn(request);
//            String result = "<div id ='graphQueryGeneratorid' class = 'graphQueryGeneratorClass'>";
			String result = "<span id='inputstrId' class='inputstrClass'>";
			result += "<input type=\"text\"id=\"inpittext\" data-opt-name=\"hole\"/>";
			result += "</span>";
			if (tablelist != null && !tablelist.isEmpty()) {
				tableStr = "<span id='tableId' class='tableClass'>";
				tableStr += "File Name:<select id ='DxpdashbordoptionListId' class='DxpdashbordoptionListClass' onChange=\"gettable(event,id)\">";
				for (int i = 0; i < tablelist.size(); i++) {
					String selected = "";
					tableStr += "<option value= '" + tablelist.get(i) + "' " + selected + ">" + tablelist.get(i)
							+ "</option>";
				}
				tableStr += "</select>";
				tableStr += "</span>";
			}
			if (columnlist != null && !columnlist.isEmpty()) {
				ColumnStr = "<select id ='columnlistoptionListId' class='columnlistoptionListClass' onChange=\"getDashBoardCharts(event,id)\">";
				for (int i = 0; i < columnlist.size(); i++) {
					String selected = "";
					ColumnStr += "<option value= '" + columnlist.get(i) + "' " + selected + ">" + columnlist.get(i)
							+ "</option>";
				}
				ColumnStr += "</select>";
			}
//            result += "</div>"; 

			resultobj.put("result", result);
			resultobj.put("tableStr", tableStr);
			resultobj.put("ColumnStr", ColumnStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	public JSONObject getModalFileColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getModalFileColumns(request);
		} catch (Exception e) {
			e.printStackTrace();         
		}
		return resultObj;
	}

	public JSONObject fetchModalChartData(HttpServletRequest request) {
		JSONObject chartObj = new JSONObject();
		try {
			String chartType = request.getParameter("chartType");
			String chartId = request.getParameter("chartId");
			if("BarAndLine".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchBarwithLineEChartData(request);
				chartObj.put("flag", "Y");
			} else if("treemap".equalsIgnoreCase(chartType) || "sunburst".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchTreeMapEChartData(request);
				chartObj.put("flag", "Y");
			} else if("heatmap".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchHeatMapEChartData(request);
				chartObj.put("flag", "Y");
			}else if (chartType != null && !"".equalsIgnoreCase(chartType) && "sankey".equalsIgnoreCase(chartType)) {
				chartObj = dashBoardsDAO.fetchSankeyChartData(request);
				chartObj.put("flag", "Y");
			}   else {
				chartObj = dashBoardsDAO.fetchModalChartData(request);   
			}

             chartObj.put("chartId", chartId);
			   

		} catch (Exception ex) {     
			ex.printStackTrace();
		}
		return chartObj;
	}

	public String renameSQLColumn(HttpServletRequest request) {
		String reuslt = "";
		try {
			reuslt = dashBoardsDAO.renameSQLColumn(request);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return reuslt;
	}

	public String getColumnformStr(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.getColumnDataType(request);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	public String getAggregateResult(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.getAggregateResult(request);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	public String createSuffixAndPriffix(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.createSuffixAndPriffix(request);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	public String updatePalatteColor(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsDAO.updatePalatteColor(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public JSONObject getDataCorrelation(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataTableArray = new JSONArray();
		try {
			JSONObject responeObject = dashBoardsDAO.getDataCorrelation(request);
			if (responeObject != null && !responeObject.isEmpty()) {
				String correlationDataStr = String.valueOf(responeObject.get("dataCorrelation"));
				List<String> headersArray = (List<String>) responeObject.get("headers");
				JSONObject correlationDataObj = (JSONObject) JSONValue.parse(correlationDataStr);
				List<JSONObject> correlatedDataList = (List<JSONObject>) correlationDataObj.keySet().stream()
						.map(e -> correlationDataObj.get(e)).collect(Collectors.toList());
				JSONArray dataFieldsArray = getDataFieldsArray(headersArray);
				JSONArray columnsArray = getColumnsArray(headersArray);
				resultObj.put("dataObject", correlatedDataList);
				resultObj.put("dataFields", dataFieldsArray);
				resultObj.put("columns", columnsArray);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONArray getDataFieldsArray(List<String> tableHeaders) {
		JSONArray dataFieldsArray = new JSONArray();
		try {
			for (String field : tableHeaders) {
				JSONObject dataFieldsObj = new JSONObject();
				dataFieldsObj.put("name", field);
				dataFieldsObj.put("type", "float");
				dataFieldsArray.add(dataFieldsObj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataFieldsArray;
	}

	public JSONArray getColumnsArray(List<String> tableHeaders) {
		JSONArray columnsArray = new JSONArray();
		try {
			for (String field : tableHeaders) {
				JSONObject columnsObj = new JSONObject();
				String replacedField = field.replace("_", " ");
				String titleCaseField = dashBoardUtills.convertTextToTitleCase(replacedField);
				columnsObj.put("type", titleCaseField);
				columnsObj.put("dataField", field);
				columnsObj.put("width", 300);
				columnsArray.add(columnsObj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return columnsArray;
	}

	public JSONObject getAutoSuggestedChartTypes(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getAutoSuggestedChartTypes(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getDateColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getDateColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getQueryGridData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getQueryGridData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getChartObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getQueryGridData(request);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject viewAnalyticsTableDataGrid(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.viewAnalyticsTableDataGrid(request);      
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject executeSQLQuery(HttpServletRequest request) {                 
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.executeSQLQuery(request);     
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getSuggestedChartTypesBasedonColumns(HttpServletRequest request) {     
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getSuggestedChartTypesBasedonColumns(request); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public String getCurrencyAndCodesData(HttpServletRequest request) {
		String currencyAndCodeHtmlData = "";
		String currencyTableHeadTrTag = "<table><tr><td>From Currency</td><td></td><td>To Currency</td></tr>";
		String fromCurrencytrTableBodyTag = "<tr><td><select id=\"fromCurrencyDropDown\">";
		String toCurrencytrTableBodyTag = "<td><select id=\"toCurrencyDropDown\">";
		StringBuilder currencyCodeList = new StringBuilder(currencyTableHeadTrTag);
		StringBuilder optionTagsForCurrencyList = new StringBuilder();
		currencyCodeList.append(fromCurrencytrTableBodyTag);
		try {
			List<Object[]> codeCurrencyList = dashBoardsDAO.getCodeAndCurrencyList(request);
			if (codeCurrencyList != null && !codeCurrencyList.isEmpty()) {
				for (Object[] object : codeCurrencyList) {
					String code = String.valueOf(object[0]);
					String currency = String.valueOf(object[1]);
					String symbol = String.valueOf(object[2]);
					String optionTag = "<option value=" + code + " data-currencySymbol=" + symbol + ">" + code
							+ "&nbsp&nbsp" + currency + "</option>";
					optionTagsForCurrencyList.append(optionTag);
				}
				currencyCodeList.append(optionTagsForCurrencyList);
				currencyCodeList.append("</td>");
				currencyCodeList.append("<td><img src='images/currency-conversion.png' width='18px'/></td>");
				currencyCodeList.append(toCurrencytrTableBodyTag);
				currencyCodeList.append(optionTagsForCurrencyList);
				currencyCodeList.append("</td></tr>");
				currencyCodeList.append("</table>");
				currencyAndCodeHtmlData = currencyCodeList.toString();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return currencyAndCodeHtmlData;
	}

	public JSONObject getAutoSuggestedFilterTables(HttpServletRequest request) {
		return dashBoardsDAO.getAutoSuggestedFilterTables(request);
	}

	public JSONObject getArtificialIntellisenseApiDetails(HttpServletRequest request) {
		return dashBoardsDAO.getArtificialIntellisenseApiDetails(request);
	}

	public JSONObject alterBiTable(HttpServletRequest request) {
		return dashBoardsDAO.alterBiTable(request);
	}

	public String gettableformStr(HttpServletRequest request) {
		return dashBoardsDAO.gettableformStr(request);
	}

	public String getSelectType(HttpServletRequest request) {
		return dashBoardsDAO.getSelectType(request);
	}

	public String getSuffixValue(HttpServletRequest request) {
		return dashBoardsDAO.getSuffixValue(request);
	}

	public String getPrefixValue(HttpServletRequest request) {
		return dashBoardsDAO.getPrefixValue(request);
	}

	public String getCreateFind(HttpServletRequest request) {
		return dashBoardsDAO.getCreateFind(request);
	}

	public String getRenameValue(HttpServletRequest request) {
		return dashBoardsDAO.getRenameValue(request);
	}

	public JSONObject executeAlterTable(HttpServletRequest request) {

		return dashBoardsDAO.executeAlterTable(request);
	}

	public String createPrefixValue(HttpServletRequest request) {
		return dashBoardsDAO.createPrefixValue(request);
	}

	public String deleterowdata(HttpServletRequest request) {
		return dashBoardsDAO.deleterowdata(request);
	}

	public JSONObject removeDuplicateValue(HttpServletRequest request) {
		return dashBoardsDAO.removeDuplicateValue(request);
	}

	public JSONObject removeDuplicateEachColumn(HttpServletRequest request) {
		return dashBoardsDAO.removeDuplicateEachColumn(request);

	}

	public JSONObject deleteDuplicateValues(HttpServletRequest request) {
		return dashBoardsDAO.deleteDuplicateValues(request);
	}

	public JSONObject executePythonQuery(HttpServletRequest request) {
		return dashBoardsDAO.executePythonQuery(request);
	}

	public JSONObject getPythonChartObjectData(HttpServletRequest request) {
		return dashBoardsDAO.getPythonChartObjectData(request);
	}

	public JSONObject getCardDateValues(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getCardDateValues(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject saveImageOnServer(HttpServletRequest request, MultipartFile multipartFileData) {
		JSONObject resultObject = new JSONObject();
		try {
			boolean isImageUploaded = false;
			String imageUploadResponse = "";
			String imageEncodedString = "";
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String fileName = StringUtils.cleanPath(multipartFileData.getOriginalFilename());
			String fileExtension = FilenameUtils.getExtension(fileName);
			String updatedImageName = "CardUploadedImage" + System.currentTimeMillis() + "." + fileExtension;
			String fileDirectoryOnserver = fileStoreHomedirectory + "images/" + userName;
			if (fileExtension != null && !"".equalsIgnoreCase(fileExtension) && !"null".equalsIgnoreCase(fileExtension)
					&& "JPEG".equalsIgnoreCase(fileExtension) || "PNG".equalsIgnoreCase(fileExtension)
					|| "SVG".equalsIgnoreCase(fileExtension) || "JPG".equalsIgnoreCase(fileExtension)) {
				isImageUploaded = dashBoardUtills.saveFileOnServer(fileDirectoryOnserver, updatedImageName,
						multipartFileData);
				if (isImageUploaded) {
//					imageUploadResponse = "Image uploaded successfully.";
					String fileContentType = multipartFileData.getContentType();
					imageEncodedString = dashBoardUtills.getImageBase64EncodedString(fileDirectoryOnserver,
							updatedImageName);
					String imageHeader = "data:" + fileContentType + ";base64,";
					resultObject.put("imageEncodedString", imageHeader + imageEncodedString);
					String homepageCardImgChngEvt = request.getParameter("homepageCardImgChngEvt");
					if (!dashBoardUtills.isNullOrEmpty(homepageCardImgChngEvt)) {
						JSONObject homepageCardImgChngEvtObj = (JSONObject) JSONValue.parse(homepageCardImgChngEvt);
						String isCardImgChngEvt = String.valueOf(homepageCardImgChngEvtObj.get("isCardImgChngEvt"));
						if (!dashBoardUtills.isNullOrEmpty(homepageCardImgChngEvt)
								&& "true".equalsIgnoreCase(isCardImgChngEvt)) {
							homepageCardImgChngEvtObj.put("encodedCardImg", imageHeader + imageEncodedString);
							int updatedCount = dashBoardsDAO.updateHomepageCardImg(request, homepageCardImgChngEvtObj);
							if (updatedCount <= 0) {
								imageUploadResponse = "Failed to upload the image.";
								resultObject.put("isImageUploaded", false);
								return resultObject;
							}
						}
					}
				} else {
					imageUploadResponse = "Failed to upload the image.";
				}
//				resultObject.put("imageUploadResponse", imageUploadResponse);
				resultObject.put("isImageUploaded", isImageUploaded);
				resultObject.put("imageName", updatedImageName);

			} else {
				imageUploadResponse = "Upload Failed, Please upload only Images.";
				resultObject.put("imageUploadResponse", imageUploadResponse);
				resultObject.put("isImageUploaded", isImageUploaded);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}

	public JSONObject getCardImageData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getCardImageData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public JSONObject getCardImgData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getCardImageData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	public JSONObject chartDxpJoinTables(HttpServletRequest request) {
        JSONObject resultObj = new JSONObject();
        JSONArray tablesObj = new JSONArray();
        // JSONObject labelObj = new VisionUtills().getMultilingualObject(request);
        String fromTable = request.getParameter("tablesObj");
        if (fromTable != null && !"".equalsIgnoreCase(fromTable) && !"null".equalsIgnoreCase(fromTable)) {
            tablesObj = (JSONArray) JSONValue.parse(fromTable);
        }
        try {
            String tabsString = "<div id='dataMigrationTabs' class='dataMigrationTabs'>"
                    //                    + "<ul class='dataMigrationTabsHeader'>"
                    //                    + "<li class='dataMigrationTabsli'><a href='#tabs-1'>" + new VisionUtills().convertIntoMultilingualValue(labelObj, "Join Clauses") + "</a></li>"
                    //                    + "</ul>"
                    + "<div id='tabs-1' class='dataMigrationsTabsInner'>"
                    + " </div>"
                    + " </div>"
                    + "";
            resultObj.put("tabsString", tabsString);
            resultObj.put("selectedJoinTables", joinDxpTransformationRules(request, tablesObj));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultObj;
    }

   

    

    public JSONObject fetchChartJoinDxpTablesData(HttpServletRequest request) {
        String joinsDataStr = "";
        JSONObject resultObj = new JSONObject();
        Connection connection = null;
        try {
            // ravi start
            String joinType = "";
            // ravi end

            JSONArray masterTablesArray = new JSONArray();
            String dbObjStr = request.getParameter("dbObj");
            String childTableName = request.getParameter("tableName");
            if (childTableName != null
                    && !"".equalsIgnoreCase(childTableName)
                    && !"null".equalsIgnoreCase(childTableName)
                    && childTableName.contains(".")) {
                childTableName = childTableName.substring(childTableName.lastIndexOf(".") + 1);
            }
            String masterTables = request.getParameter("sourceTables");
            if (masterTables != null && !"".equalsIgnoreCase(masterTables) && !"".equalsIgnoreCase(masterTables)) {
                masterTablesArray = (JSONArray) JSONValue.parse(masterTables);
            }
            String joinColumnMapping = request.getParameter("joinColumnMapping");
            JSONObject joinColumnMappingObj = new JSONObject();
            if (joinColumnMapping != null && !"".equalsIgnoreCase(joinColumnMapping) && !"null".equalsIgnoreCase(joinColumnMapping)) {
                joinColumnMappingObj = (JSONObject) JSONValue.parse(joinColumnMapping);
            }
            //String trString = "<tr>";
            List<Object[]> childTableColumnList = new ArrayList<>();
            childTableColumnList = dashBoardsDAO.getTreeOracleTableColumns(request, childTableName);

            JSONArray childTableColsTreeArray = new JSONArray();
            if (childTableColumnList != null && !childTableColumnList.isEmpty()) {
                JSONObject tableObj = new JSONObject();
                tableObj.put("id", childTableName);//CONNECTION_NAME
                tableObj.put("text", childTableName);
                tableObj.put("value", childTableName);
                tableObj.put("icon", "images/GridDB.png");
                childTableColsTreeArray.add(tableObj);
                for (int i = 0; i < childTableColumnList.size(); i++) {
                    Object[] childColsArray = childTableColumnList.get(i);
                    if (childColsArray != null && childColsArray.length != 0) {
                        JSONObject columnObj = new JSONObject();
                        columnObj.put("id", childColsArray[0] + ":" + childColsArray[1]);
                        columnObj.put("text", childColsArray[1]);
                        columnObj.put("value", childColsArray[0] + ":" + childColsArray[1]);
                        columnObj.put("parentid", childColsArray[0]);
                        childTableColsTreeArray.add(columnObj);
                    }

                }
            }
            resultObj.put("childTableColsArray", childTableColsTreeArray);

            // ravi start
            String trString = "<tr>";
            String singleTrString = "<tr>";
            singleTrString += "<td width='5%'><img src=\"images/Detele Red Icon.svg\" onclick='deleteSelectedRow(this)'  class=\"visionTdETLIcons\""
                    + " title=\"Delete\" style=\"width:15px;height: 15px;cursor:pointer;\"/>"
                    + "</td>";
            singleTrString += "<td width='35%' class=\"sourceJoinColsTd\"><input class='visionColJoinMappingInput' type='text' value='' readonly='true'/>"
                    + "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
                    + " onclick=\"selectDxpColumn(this,'childColumn')\" style=\"\"></td>";

            singleTrString += "<td width='10%' class=\"sourceJoinColsTd\">"
                    + "<select id=\"OPERATOR_TYPE\"  class=\"sourceColsJoinSelectBox\">"
                    + "<option  value='=' selected>=</option>"
                    + "<option  value='!='>!=</option>"
                    + "</select>"
                    + "</td>";

            singleTrString += "<td width='35%' class=\"sourceJoinColsTd\"><input class='visionColJoinMappingInput' type='text' value='' readonly='true'/>"
                    + "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
                    + " onclick=\"selectDxpColumn(this,'masterColumn')\" style=\"\"></td>";
            singleTrString += "<td width='10%'><input type=\"text\" class=\"defaultValues\" id=\"static_value_0\"></td>"
                    + "<td width='5%'>"
                    + "<select id='andOrOpt'>"
                    + "<option value='AND'>AND</option>"
                    + "<option value='OR'>OR</option>"
                    + "</select>"
                    + "</td>";
            singleTrString += "</tr>";

            // ravi end
            // ravi start
            JSONArray masterTableColsTreeArray = new JSONArray();
            for (int i = 0; i < masterTablesArray.size(); i++) {
                String masterTableName = (String) masterTablesArray.get(i);
//                if (request.getParameter("tableName") != null && !"".equalsIgnoreCase(request.getParameter("tableName"))
//                        && !childTableName.equalsIgnoreCase(request.getParameter("tableName"))) {
                if (masterTableName != null
                        && !"".equalsIgnoreCase(masterTableName)
                        && !"null".equalsIgnoreCase(masterTableName)
                        && masterTableName.contains(".")) {
                    masterTableName = masterTableName.substring(masterTableName.lastIndexOf(".") + 1);
                }
                List<Object[]> columnList = new ArrayList<>();
                columnList = dashBoardsDAO.getTreeOracleTableColumns(request, masterTableName);

                if (columnList != null && !columnList.isEmpty()) {
                    JSONObject tableObj = new JSONObject();
                    tableObj.put("id", masterTableName);
                    tableObj.put("text", masterTableName);
                    tableObj.put("value", masterTableName);
                    tableObj.put("icon", "images/GridDB.png");
                    masterTableColsTreeArray.add(tableObj);
                    for (int j = 0; j < columnList.size(); j++) {
                        Object[] masterColsArray = columnList.get(j);
                        if (masterColsArray != null && masterColsArray.length != 0) {
                            JSONObject columnObj = new JSONObject();
                            columnObj.put("id", masterColsArray[0] + ":" + masterColsArray[1]);
                            columnObj.put("text", masterColsArray[1]);
                            columnObj.put("value", masterColsArray[0] + ":" + masterColsArray[1]);
                            columnObj.put("parentid", masterColsArray[0]);
                            masterTableColsTreeArray.add(columnObj);
                        }

                    }
                }
//                }
            }
            resultObj.put("masterTableColsArray", masterTableColsTreeArray);
            trString = singleTrString;

// ravi end 
            //String joinType = "";
            String mappedColTrString = "";
            if (joinColumnMappingObj != null && !joinColumnMappingObj.isEmpty()) {
                Set keySet = joinColumnMappingObj.keySet();
                List keysList = new ArrayList();
                keysList.addAll(keySet);
                Collections.sort(keysList);

                for (int i = 0; i < keysList.size(); i++) {
                    Object keyName = keysList.get(i);
                    JSONObject joinColMapObj = (JSONObject) joinColumnMappingObj.get(keysList.get(i));
                    if (joinColMapObj != null && !joinColMapObj.isEmpty()) {
                        joinType = (String) joinColMapObj.get("joinType");
                        mappedColTrString
                                += "<td width='5%' ><img src=\"images/Detele Red Icon.svg\" onclick='deleteSelectedRow(this)'  class=\"visionTdETLIcons\""
                                + " title=\"Delete\" style=\"width:15px;height: 15px;cursor:pointer;\"/>"
                                + "</td>";
                        mappedColTrString += "<td width='35%' class=\"sourceJoinColsTd\">"
                                + "<input class='visionColJoinMappingInput' type='text' value='" + (String) joinColMapObj.get("childTableColumn") + "' readonly='true'/>"
                                + "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
                                + " onclick=\"selectDxpColumn(this,'childColumn')\" style=\"\"></td>";

                        String operator = (String) joinColMapObj.get("operator");

                        mappedColTrString += "<td width='10%' class=\"sourceJoinColsTd\">"
                                + "<select id=\"OPERATOR_TYPE\"  class=\"sourceColsJoinSelectBox\">";
                        mappedColTrString += "<option  value='=' " + ("=".equalsIgnoreCase(operator) ? "selected" : "") + ">=</option>";
                        mappedColTrString += "<option  value='!=' " + ("!=".equalsIgnoreCase(operator) ? "selected" : "") + ">!=</option>";
                        mappedColTrString += "</select>"
                                + "</td>";
                        mappedColTrString += "<td width='35%' class=\"sourceJoinColsTd\">"
                                + "<input class='visionColJoinMappingInput' type='text' value='" + (String) joinColMapObj.get("masterTableColumn") + "' readonly='true'/>"
                                + "<img title='Select Column' src=\"images/tree_icon.svg\" class=\"visionETLColMapImage \" "
                                + " onclick=\"selectDxpColumn(this,'masterColumn')\" style=\"\"></td>";
                        mappedColTrString += ""
                                + "<td width='10%'><input type=\"text\" "
                                + "value='" + ((joinColMapObj.get("staticValue") != null
                                && !"".equalsIgnoreCase(String.valueOf(joinColMapObj.get("staticValue")))
                                && !"null".equalsIgnoreCase(String.valueOf(joinColMapObj.get("staticValue"))))
                                ? String.valueOf(joinColMapObj.get("staticValue")) : "") + "' "
                                + " class=\"defaultValues\" id=\"static_value_" + i + "\"></td>"
                                + "<td width='5%'>"
                                + "<select id='andOrOpt'>";
                        String andOrOperator = (String) joinColMapObj.get("andOrOperator");
                        mappedColTrString += "<option value='AND' " + ("AND".equalsIgnoreCase(andOrOperator) ? "selected" : "") + ">AND</option>";
                        mappedColTrString += "<option value='OR' " + ("OR".equalsIgnoreCase(andOrOperator) ? "selected" : "") + ">OR</option>";
                        mappedColTrString += "</select>"
                                + "</td>";
                        mappedColTrString += "</tr>";
                    }

                }
            }
            if (!(mappedColTrString != null
                    && !"".equalsIgnoreCase(mappedColTrString)
                    && !"null".equalsIgnoreCase(mappedColTrString))) {
                mappedColTrString = trString;
            }
            joinsDataStr += "<div class=\"visionEtlJoinClauseMain visionAnalyticsJoinClauseMain\">"
                    + "<div class=\"visionEtlAddIconDiv\">"
                    + "<img data-trstring='' src=\"images/Add icon.svg\" id=\"visionDxpAddRowIcon\" "
                    + "class=\"visionDxpAddRowIcon\" title=\"Add column for mapping\""
                    + " onclick=addNewDxpJoinsRow(event,'" + dbObjStr + "',id) "
                    + "style=\"width:15px;height: 15px;cursor:pointer; float: left;\"/>"
                    + "<img data-trstring='' src=\"images/Save Icon.svg\" id=\"visionEtlSaveIcon\" "
                    + "class=\"visionDxpAddRowIcon\" title=\"Save Mapping\""
                    + " onclick=saveDxpJoinMapping(event,id) "
                    + "style=\"width:15px;height: 15px;cursor:pointer; float: left;\"/>"
                    + "<span class='visionDxpColumnJoinType'>Join Type : </span>"
                    + "<select class='visionDxpColumnJoinType' id='joinType'>"
                    + "<option value='INNER JOIN' " + ("INNER JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + " >Inner Join</option>"
                    + "<option value='JOIN' " + ("JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + ">Join</option>"
                    + "<option value='LEFT OUTER JOIN' " + ("LEFT OUTER JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + ">Left Outer Join</option>"
                    + "<option value='RIGHT OUTER JOIN' " + ("RIGHT OUTER JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + ">Right Outer Join</option>"
                    + "<option value='OUTER JOIN' " + ("OUTER JOIN".equalsIgnoreCase(joinType) ? "selected" : "") + ">Outer Join</option>"
                    + "</select>"
                    + "</div>"
                    + "<div class=\"visionDxpJoinClauseTablesDiv\">"
                    + "<table class=\"visionEtlJoinClauseTable\" id='etlJoinClauseTable' style='width: 100%;' border='1'>"
                    + "<thead>"
                    + "<tr>"
                    + "<th width='5%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'></th>"
                    + "<th width='35%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Child Column</th>"
                    + "<th width='10%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Operator</th>"
                    + "<th width='35%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Master Column</th>"
                    + "<th width='10%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>Static Value</th>"
                    + "<th width='5%' style='background: #f1f1f1 none repeat scroll 0 0;text-align: center'>AND/OR</th>"
                    + ""
                    + "</tr>"
                    + "</thead>"
                    + "<tbody>"
                    + "";
            joinsDataStr += mappedColTrString
                    + "</tbody>"
                    + ""
                    + "</table>"
                    + ""
                    + "</div>"
                    + "</div>";
            resultObj.put("joinsDataStr", joinsDataStr);
            resultObj.put("trString", singleTrString); // ravi edit

        } catch (Exception e) {    
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) { 
                    connection.close();
                }
            } catch (Exception e) { 
            }
        }
        return resultObj;    

    }
    
    public String importTreeDMFile(HttpServletRequest request, HttpServletResponse response, MultipartFile file1, String selectedFiletype) {

        String result = "";
        String filename = "";
        JSONObject importResult = new JSONObject();
        try {
            String excelFilePath = etlFilePath+"Files/TreeDMImport/" + request.getSession(false).getAttribute("ssUsername");
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            File file = new File(excelFilePath);
            if (file.exists()) {
                file.delete();
            }
            if (!file.exists()) {
                file.mkdirs();
            }
            DiskFileItemFactory factory = new DiskFileItemFactory();
            // maximum size that will be stored in memory
            factory.setSizeThreshold(maxMemSize);
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setSizeMax(maxFileSize);
            List fileItems = upload.parseRequest(request);
            byte[] bytes = file1.getBytes();
            filename = file1.getOriginalFilename();
            System.out.println("filenAME:::" + filename);
            String fileType1 = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
            String mainFileName = "SPIRUploadSheet" + System.currentTimeMillis() + "." + fileType1;
            selectedFiletype = selectedFiletype.toLowerCase();
            fileType1 = fileType1.toLowerCase();
            
            if (selectedFiletype != null && !"".equalsIgnoreCase(selectedFiletype) && fileType1 != null
                    && !"".equalsIgnoreCase(fileType1) && !selectedFiletype.equalsIgnoreCase(fileType1)) {
                result = "Please upload " + selectedFiletype + " files only";
                importResult.put("result", result);
                importResult.put("flag", "Fail");
            } else {
                if (filename != null) {
                    if (filename.lastIndexOf(File.separator) >= 0) {

                        file = new File(filename);
                    } else {
                        file = new File(excelFilePath + File.separator + mainFileName);
                     }

                    FileOutputStream osf = new FileOutputStream(file);

                    osf.write(bytes);
                    osf.flush();
                    osf.close();
                    
                    HttpHeaders headers = new HttpHeaders();
                    //headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    headers.setAccept(Collections.singletonList(MediaType.MULTIPART_FORM_DATA));
        			FileSystemResource fileData = new FileSystemResource(file);
        			MultiValueMap inputMap = new LinkedMultiValueMap();
        			inputMap.add("fileName", fileData); 
        			inputMap.add("flag", "Y"); 
        			//inputMap.add("Content-disposition", "form-data; name=file; filename="+mainFileName+"");
        			//inputMap.add("Content-type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        			String dataCorrelaltionApiUrl = "http://idxp.pilogcloud.com:6649/file/";
        			HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<MultiValueMap<String, Object>>(inputMap,
        					headers);
        			RestTemplate template = new RestTemplate();
        			ResponseEntity<byte[]> apiResponse = template.postForEntity(dataCorrelaltionApiUrl, entity,
        					byte[].class);
        			byte[] apiDataObj = apiResponse.getBody();
        			if(apiDataObj !=null)
        			{
        				file.delete();
        				mainFileName = "SPIRUploadSheet" + System.currentTimeMillis() + "." + fileType1;
        				FileOutputStream output = new FileOutputStream(new File(excelFilePath + File.separator + mainFileName));
        				filename = excelFilePath + File.separator + mainFileName;
        				IOUtils.write(apiDataObj, output);
        			}
        			

                   
                    try {
                     // dashBoardsDAO.saveUserFiles(request, originalFileName, mainFileName, filePath, selectedFiletype);
  				} catch (Exception e) {
  				}
  				String gridId = "divGrid-" + mainFileName.replace("." + selectedFiletype, "");
  				gridId = gridId.replace(".csv", "");

  				  importResult = getFileObjectMetaData(request, response, filename, gridId, selectedFiletype,
  						mainFileName);
                } else {
                    result = "[]";
                    importResult.put("result", result);
                    importResult.put("flag", "Fail"); 
                }
            }

            importResult.put("fileName", mainFileName);
            importResult.put("fileType", fileType1); 
        } catch (Exception e) {
            e.printStackTrace();
        }

        return importResult.toJSONString();  
    }
    
    public JSONObject getChatBotResponse(HttpServletRequest request) { 
		JSONObject resultObj = new JSONObject();
        try {
//            String gridId = request.getParameter("gridId");
            String message = (String) request.getParameter("message");
            String username = (String) request.getParameter("username");
            String sessionId = (String) request.getParameter("sessionId");
            String lang = (String) request.getParameter("lang");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> inputMap = new LinkedMultiValueMap<>(); 
            inputMap.add("msg", message);
            inputMap.add("user_name", sessionId);
            inputMap.add("lang", lang);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(inputMap,
                    headers);
            RestTemplate template = new RestTemplate();
            ResponseEntity<JSONObject> response = template.postForEntity("http://idxp.pilogcloud.com:6653/chatbot/", entity,
                    JSONObject.class);
            JSONObject apiDataObj = response.getBody(); 
            if (apiDataObj != null && !apiDataObj.isEmpty()) {
            	if(!(apiDataObj.get("says") !=null && !((ArrayList)apiDataObj.get("says")).isEmpty()))
            	{
            		JSONArray saysArr = new JSONArray();
            		saysArr.add("Please select the below options");
            		apiDataObj.put("says",saysArr );
            	}
            	resultObj.put("ice", apiDataObj); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultObj;
    }
    
    
	public JSONObject getUserTableNames(HttpServletRequest request) {
        JSONObject resultObj = new JSONObject();
		try {
			String editorFlag = request.getParameter("editorFlag");
			if(editorFlag !=null && !"".equalsIgnoreCase(editorFlag) && "Y".equalsIgnoreCase(editorFlag))
			{
				resultObj = dashBoardsDAO.getEditorViewUserTableNames(request);
			}else {
				resultObj = dashBoardsDAO.getUserTableNames(request);                                       
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}
	
	
	
	public JSONObject importIntelliSenseTreeDMFileXlsx(HttpServletRequest request, HttpServletResponse response,
			JSONObject jsonData, String selectedFiletype) {

		FileInputStream inputStream = null;
		FileOutputStream outs = null;
		JSONObject importResult = new JSONObject();
		try {
			if (true) {
				// fis = new FileInputStream(new File(filepath));
				String originalFileName = request.getParameter("fileName");
				String userName = (String) request.getSession(false).getAttribute("ssUsername");
				String filePath = fileStoreHomedirectory + "TreeDMImport/" + userName;
//				String filePath = "C:/Files/TreeDMImport" + File.separator + userName;

				String mainFileName = "SPIRUploadSheet" + System.currentTimeMillis() + "." + selectedFiletype;
				String fileName = filePath + File.separator + mainFileName;

				String headersObjStr = request.getParameter("headersObj");
				JSONObject headersObj = (JSONObject) JSONValue.parse(headersObjStr);

				String sheetsStr = request.getParameter("sheets");
				JSONArray sheetsArray = (JSONArray) JSONValue.parse(sheetsStr);

				File outputFile = new File(filePath);
				if (outputFile.exists()) {
					outputFile.delete();
				}
				if (!outputFile.exists()) {
					outputFile.mkdirs();
				}

				XSSFWorkbook outputWb = new XSSFWorkbook();
//                Workbook outputWb = (XSSFWorkbook) WorkbookFactory.create(new File(fileName));
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				CellStyle dateCellStyle = outputWb.createCellStyle();
				CellStyle timeCellStyle = outputWb.createCellStyle();
				CreationHelper createHelper = outputWb.getCreationHelper();
				dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
				timeCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("h:mm:ss"));

				for (Object sheet : sheetsArray) {
					XSSFSheet outputSheet = outputWb.createSheet((String) sheet);

					JSONArray sheetData = (JSONArray) jsonData.get(sheet);
					JSONArray sheetHeaders = (JSONArray) headersObj.get(sheet);
					XSSFRow outPutHeader = outputSheet.createRow(0);

					for (int cellIndex = 0; cellIndex < sheetHeaders.size(); cellIndex++) {

						WritableFont cellFont = new WritableFont(WritableFont.TIMES, 16);

						WritableCellFormat cellFormat = new WritableCellFormat(cellFont);
						cellFormat.setBackground(Colour.ORANGE);
						XSSFCellStyle cellStyle = outputWb.createCellStyle();
						cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
						cellStyle.setWrapText(true);

						String cellValue = (String) sheetHeaders.get(cellIndex);

						XSSFCell hssfCell = outPutHeader.createCell(cellIndex);
						hssfCell.setCellStyle(cellStyle);

						hssfCell.setCellValue(cellValue);

					}

					for (int i = 0; i < sheetData.size(); i++) {

						XSSFRow outPutRow = outputSheet.createRow(i + 1);

						JSONObject rowData = (JSONObject) sheetData.get(i);
						if (rowData != null) {

							for (int cellIndex = 0; cellIndex < sheetHeaders.size(); cellIndex++) {
								String header = (String) sheetHeaders.get(cellIndex);
								Object cellValue = rowData.get(header);
								XSSFCell outputCell = null;
								try {
//                            System.out.println(i+ " cellIndex::::" + cellIndex);
									outputCell = outPutRow.createCell(cellIndex);
									if (cellValue != null) {

										if (cellValue instanceof String) {
											if (isValidDate((String) cellValue)) {
												Date date = sdf.parse((String) cellValue);
												outputCell.setCellValue(date);
												if (((String) cellValue).contains("1899-12-31T")) {
													String timeStr = ((String) cellValue).substring(11, 19);
													Double timeDouble = DateUtil.convertTime(timeStr);
													outputCell.setCellValue(timeDouble);
													outputCell.setCellStyle(timeCellStyle);
												} else {
													outputCell.setCellStyle(dateCellStyle);
												}

											} else {
												outputCell.setCellValue((String) cellValue);
											}

//                                            outputCell.setCellType(CellType._NONE);
										} else if (cellValue instanceof Number) {
											outputCell.setCellValue(Double.valueOf(String.valueOf(cellValue)));
										} else if (cellValue instanceof Boolean) {
											outputCell.setCellValue((Boolean) cellValue);
										} else {
											outputCell.setCellValue(String.valueOf(cellValue));
										}

									} else {
										outputCell.setCellValue("");
									}

								} catch (Exception e) {
									outputCell.setCellValue("");
									continue;
								}

							}
						}

					}
				}
				outs = new FileOutputStream(fileName);
				outputWb.write(outs);
				outs.close();
				
				importResult = createIntelliSenseTableasFile(request,response,mainFileName);
			}
			// return result1;
			if (inputStream != null) {
				inputStream.close();
			}
			
			

		} catch (Exception e) {
			
			e.printStackTrace();
		}

		return importResult;      
	}
	
	public JSONObject createIntelliSenseTableasFile(HttpServletRequest request, HttpServletResponse response,String mainFileName)  {
		JSONObject resultObj = new JSONObject();
		JSONArray checkBoxDataArr = new JSONArray();
		try {
			resultObj = dashBoardsDAO.createIntelliSenseTableasFile(request,response,mainFileName);  
			
        } catch (Exception e) {
			e.printStackTrace();
		}
		 
		return resultObj;      
	}
	
	public JSONObject getIntelliSenseTableColumns(HttpServletRequest request) {          
		JSONObject resultObj = new JSONObject();
		JSONArray checkBoxDataArr = new JSONArray();
		try {
			String editorFlag = request.getParameter("editorFlag");
			if(editorFlag !=null && !"".equalsIgnoreCase(editorFlag) && "Y".equalsIgnoreCase(editorFlag))
			{
				resultObj = dashBoardsDAO.getEditorViewTableColumns(request); 
			}else {
				resultObj = dashBoardsDAO.getIntelliSenseTableColumns(request);                             
			}
			               
			
        } catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject getIntelliSenseChartTypes(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();
		JSONArray checkBoxDataArr = new JSONArray();      
		try {
			resultObj = dashBoardsDAO.getIntelliSenseChartTypes(request);            
		} catch (Exception e) {
			e.printStackTrace();      
		}
		return resultObj;
	}
	
	public JSONObject getIntelliSenseChartColumns(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();      
		try {
			resultObj = dashBoardsDAO.getIntelliSenseChartColumns(request); 
			} catch (Exception e) {  
			e.printStackTrace();        
		}
		return resultObj;                                        
	}            
	public JSONObject getIntelliSenseChartConfig(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();      
		try {
			resultObj.put("configOptions",getChartFilters(request));              
			
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject getIntelliSenseExampleChartDesign(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getIntelliSenseExampleChartDesign(request);            
			} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;       
	}
	
	public JSONObject getIntelliSenseChartSubColumns(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsDAO.getIntelliSenseChartSubColumns(request);                        
			} catch (Exception e) {
			e.printStackTrace();                 
		}
		return resultObj;   
	}
	
	public JSONObject getIntelliSenseViewFilters(HttpServletRequest request) {               
		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = dashBoardsDAO.getIntelliSenseViewFilters(request);              
			} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	public JSONObject getIntelliSenseViewFiltersValues(HttpServletRequest request) {                     
		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = dashBoardsDAO.getIntelliSenseViewFiltersValues(request);              
			} catch (Exception e) {   
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject getEditorMergeTableNames(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = dashBoardsDAO.getEditorMergeTableNames(request);                       
		} catch (Exception e) {   
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject getEditorMergeTableColumns(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = dashBoardsDAO.getEditorMergeTableColumns(request);                 
		} catch (Exception e) {   
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject checkExistMergeTableName(HttpServletRequest request) {  
		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = dashBoardsDAO.checkExistMergeTableName(request);                        
		} catch (Exception e) {   
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject createTableANdJoinTables(HttpServletRequest request) {   
		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = chartJoinTables(request);
			resultObj.put("tableDiv",dashBoardsDAO.createTableANdJoinTables(request));
			
		} catch (Exception e) {   
			e.printStackTrace();
		}
		return resultObj;
	}
	public JSONObject insertMergeTablesData(HttpServletRequest request) {      
		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = dashBoardsDAO.insertMergeTablesData(request);
			
		} catch (Exception e) {   
			e.printStackTrace();
		}
		return resultObj;
	}
	
	public JSONObject getChatRplyResponse(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String message = (String) request.getParameter("message");
			String sessionId = (String) request.getParameter("sessionId");
			String lang = (String) request.getParameter("lang");
			String ssUsername = (String) request.getSession(false).getAttribute("ssUsername");
			String mainDiv = "";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> inputMap = new LinkedMultiValueMap(); 
			inputMap.add("msg", message); 
			inputMap.add("sessionId", sessionId);   
			inputMap.add("name", ssUsername); 
			/* inputMap.add("lang", lang); */
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(inputMap,
					headers);
			RestTemplate template = new RestTemplate();
			ResponseEntity<JSONObject> response = template.postForEntity("http://idxp.pilogcloud.com:6658/txtsql/",
					entity, JSONObject.class);
			JSONObject apiDataObj = response.getBody(); 

			resultObj.put("result", apiDataObj);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	public JSONObject getConvAIMergeTableColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
           resultObj = dashBoardsDAO.getConvAIMergeTableColumns(request);
        } catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	public String updateCardData(HttpServletRequest request) {
		return dashBoardsDAO.updateCardData(request);
	}
	
	public String deleteDashboard(HttpServletRequest request) {
		return dashBoardsDAO.deleteDashboard(request);
	}
	
	public JSONObject saveFileOnServer(HttpServletRequest request, MultipartFile multipartFileData) {
		JSONObject fileObj = new JSONObject();
		try {
			String userName = (String) request.getSession(false).getAttribute("ssUsername");
			String fileName = StringUtils.cleanPath(multipartFileData.getOriginalFilename());
			String fileExtension = FilenameUtils.getExtension(fileName);
			String updatedFileName = "SPIRUploadSheet" + System.currentTimeMillis() + "." + fileExtension;
			String fileDirectoryOnserver = etlFilePath + "Files/TreeDMImport/" + userName;
			boolean isFileUploaded = dashBoardUtills.saveFileOnServer(fileDirectoryOnserver, updatedFileName, multipartFileData);
			if (!isFileUploaded) {
				fileObj.put("uploadStatus", "false");
				return fileObj;
			}
			fileObj.put("uploadStatus", "true");
			fileObj.put("originalFileName", fileName);
			fileObj.put("updatedFileName", updatedFileName);
			fileObj.put("fileExtension", fileExtension);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileObj;
	}
	
	public JSONObject getEditDashBoardNames(HttpServletRequest request) {
		return dashBoardsDAO.getEditDashBoardNames(request);
	}
	
	public JSONObject getSaveDashBoardNames(HttpServletRequest request) {
		return dashBoardsDAO.getSaveDashBoardNames(request);
	}
	
	public JSONObject getWeatherDetailsByCity(HttpServletRequest request)  {
        JSONObject resultObj = new JSONObject();
        
        RestTemplate restTemplate = new RestTemplate();
        String city = request.getParameter("city");
        String flag = request.getParameter("flag");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("city", city); 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.MULTIPART_FORM_DATA));
        HttpEntity<MultiValueMap<String, String>> requestEntity =new HttpEntity<MultiValueMap<String, String>>(formData,
                headers);


        String apiUrl = "http://idxp.pilogcloud.com:6671/weather_report/"; // Replace with the API endpoint URL
        ResponseEntity<JSONObject> response = restTemplate.postForEntity(apiUrl,requestEntity, JSONObject.class);
        JSONObject  responseBody = response.getBody();
        String htmlDiv = "";
        LocalDate currentDate = LocalDate.now();
        
        // Define a custom pattern for the month format
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);
        
        // Format the month of the current date
        String formattedMonth = monthFormatter.format(currentDate);
        
        // Create a custom format pattern for the entire date
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH);
        
        // Format the entire current date using the custom pattern
        String formattedDate = customFormatter.format(currentDate);
        
        // Combine the formatted date and location
        String location = "Hyderabad, India";
        String result = formattedDate + " " + location;
    	LocalDate currentDatee = LocalDate.now();
        DayOfWeek dayOfWeek = currentDatee.getDayOfWeek();
        if(responseBody !=null && !responseBody.isEmpty()) 
        {
        	if (flag == null || flag.isEmpty() || !flag.equalsIgnoreCase("HD")) {
        	Date todayDate = new Date();
        	
        	 htmlDiv = "<div class=\"weatherContent page-container\" id=\"weatherContent\">\r\n"
        	 		+ "        <div class=\"mainWrapperDiv\">\r\n"
        	 		+ "            <div class=\"subWrapper\">\r\n"
        	 		+ "    <div class=\"grid-margin stretch-card\">\r\n"
        	 		+ "                  <!--weather card-->\r\n"
        	 		+ "                  <div class=\"card card-weather\">\r\n"
        	 		+ "                    <div class=\"card-body cardInMainDiv weatherMiddleInnerContent\">\r\n"
        	 		+ "                      <div class=\"temp_Location\">\r\n"
        	 		+ "                        <div class=\"weather-date-location\">\r\n"
        	 		+ "                          <h3>"+dayOfWeek+"</h3>\r\n"
        	 		+ "                          <p class=\"text-gray\">\r\n"
        	 		+ "                            <span class=\"weather-date\">"+result+"</span>\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                        <div class=\"weather-data\">\r\n"
        	 		+ "                          <div class=\"mr-auto\">\r\n"
        	 		+ "                            <h4 class=\"display-3\">"+responseBody.get("temp_celsius")+"\r\n"
        	 		+ "                              <sup class=\"symbol\">&deg;</sup>C</h4>\r\n"
        	 		+ "                            <p>\r\n"
        	 		+ "                              "+responseBody.get("description")+"\r\n"
        	 		+ "                            </p>"
        	 		+ "                          </div>"
        	 		+ "                          <div class='weatherReport'><div>Precipitation:"+responseBody.get("feels_like_celsius")+"%</div><div>Humidity:"+responseBody.get("humidity")+"%</div><div>Wind:"+responseBody.get("wind")+"km/h</div></div>"
        	 		+ "                        </div>\r\n"
        	 		+ "                      </div>\r\n"
        	 		+ "                      <div class=\"changerContent\"> \r\n"
        	 		+ "                        <div class=\"changeButtonMainDiv\">\r\n"
        	 		+ "                          <div class=\"buttonClass active-link\" onclick=\"opentab('temp')\">Temperature</div>\r\n"
        	 		+ "                          <div class=\"buttonClass\" onclick=\"opentab('prec')\">Precipitation</div>\r\n"
        	 		+ "                          <div class=\"buttonClass\" onclick=\"opentab('wind')\">Wind</div>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                          <div class=\"passDataInfo active-tab\" id=\"temp\">\r\n"
        	 		+ "                            <span><img src='images/temp_img.png'></span>\r\n"
        	 		+ "                          </div>\r\n"
        	 		+ "                          <div class=\"passDataInfo\" id=\"prec\">\r\n"
        	 		+ "                           <span><img src='images/perception.png'></span>\r\n"
        	 		+ "                          </div>\r\n"
        	 		+ "                          <div class=\"passDataInfo\" id=\"wind\">\r\n"
        	 		+ "                            <span><img src='images/wind.png'></span>\r\n"
        	 		+ "                          </div>\r\n"
        	 		+ "                      </div>\r\n"
        	 		+ "                    </div>\r\n"
        	 		+ "                      <div class=\"bottomWeatherIcons weakly-weather\">\r\n"
        	 		+ "                        <div class=\"weakly-weather-item\">\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            Sun\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                          <i class=\"mdi mdi-weather-cloudy\"></i>\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            30&deg;\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                        <div class=\"weakly-weather-item\">\r\n"
        	 		+ "                          <p class=\"mb-1\">\r\n"
        	 		+ "                            Mon\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                          <i class=\"mdi mdi-weather-hail\"></i>\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            31&deg;\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                        <div class=\"weakly-weather-item\">\r\n"
        	 		+ "                          <p class=\"mb-1\">\r\n"
        	 		+ "                            Tue\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                          <i class=\"mdi mdi-weather-partlycloudy\"></i>\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            28&deg;\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                        <div class=\"weakly-weather-item\">\r\n"
        	 		+ "                          <p class=\"mb-1\">\r\n"
        	 		+ "                            Wed\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                          <i class=\"mdi mdi-weather-pouring\"></i>\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            30&deg;\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                        <div class=\"weakly-weather-item\">\r\n"
        	 		+ "                          <p class=\"mb-1\">\r\n"
        	 		+ "                            Thu\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                          <i class=\"mdi mdi-weather-pouring\"></i>\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            29&deg;\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                        <div class=\"weakly-weather-item\">\r\n"
        	 		+ "                          <p class=\"mb-1\">\r\n"
        	 		+ "                            Fri\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                          <i class=\"mdi mdi-weather-snowy-rainy\"></i>\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            31&deg;\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                        <div class=\"weakly-weather-item\">\r\n"
        	 		+ "                          <p class=\"mb-1\">\r\n"
        	 		+ "                            Sat\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                          <i class=\"mdi mdi-weather-snowy\"></i>\r\n"
        	 		+ "                          <p class=\"mb-0\">\r\n"
        	 		+ "                            32&deg;\r\n"
        	 		+ "                          </p>\r\n"
        	 		+ "                        </div>\r\n"
        	 		+ "                      </div>\r\n"
        	 		+ "                    </div>\r\n"
        	 		+ "                  <!--weather card ends-->\r\n"
        	 		+ "                </div>\r\n"
        	 		+ "                </div>\r\n"
        	 		+ "                </div>\r\n"
        	 		+ "                </div>";
        	 resultObj.put("response", htmlDiv);

        	}
        	else {        
                resultObj.put("temperature",responseBody.get("temp_celsius"));
                resultObj.put("description", responseBody.get("description"));
                resultObj.put("dayOfWeek", dayOfWeek);
                resultObj.put("sunrise", responseBody.get("sunrise_time"));
                resultObj.put("sunset", responseBody.get("sunset_time"));
                }
        }
        
        return resultObj;
    }
	
	public JSONObject getChartNotes(HttpServletRequest request) { 
		return dashBoardsDAO.getChartNotes(request);
	}
	
	public JSONObject saveChartNotes(HttpServletRequest request) {
		return dashBoardsDAO.saveChartNotes(request);
	}
	public String getAreaPieacesDiv(String chartType) {
		return ("<li id=\"addingLowerAndUpperBound\" class=\"general-filters active-filter\" data-column-name=\"" +
	                              ""+chartType+"AREAPIECES\" data-key-type=\"data\">" +
	                              "<p style=\"display: block;\" >Area Pieces</p><br>"
	                              +"<div class=\"sub-filterItems\">"
	                              + "      <input placeholder=\" Lower Bound\" type=\"number\" id=\""+chartType+"LOWERBOUND_0\" data-opt-name=\"width\" data-man=\"O\" title=\"\" style=\"width:50px\"> \r\n"
	                              + "      <input placeholder=\" Upper Bound\" type=\"number\" id=\""+chartType+"UPPERBOUND_0\"\r\n"
	                              + "       data-opt-name=\"width\" data-man=\"O\" title=\"\" style=\"width:50px\"> \r\n"
	                              + "<span style=\"width: 20px height: 20px\" id=\"plusbuttonInAreaPieces_0\" onclick=\"handlePlusInAreaPieces(event)\">+</span>"
	                              +"<span style=\"display:none;width: 20px height: 20px\"  id=\"minusbuttonInAreaPieces_0\" onclick=\"handleMinusInAreaPieces(event)\">-</span>"
	                              + "\n</div></li>");
	}

	public StringBuilder getLineColorProperties(String ChartType, String mode) {
		StringBuilder eChartProperties = new StringBuilder();
		eChartProperties.append("<li id=\"line-filter\" data-column-name=\""+ChartType+"\" data-key-type=\"data\">")
		        .append("<div class=\"main-container\"><div class=\"filter-container\">")
		        .append("<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>")
		        .append("<p>Chart Line</p>").append("</div>").append(getToggleButton(ChartType, "")).append("</div>")
		        .append("<ul class=\"sub-filters\" id=\""+ChartType+"\" data-opt-name=\"line\" style=\"display: none;\">")
		        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+ChartType+"COLORS\" data-key-type=\"data\">")
		        .append("<label>Line Color</label>").append("<input type=\"hidden\" id=\""+ChartType+"LINECOLORS\" value=\"#1864ab\">")
		        .append("<input type=\"color\" id=\""+ChartType+"LINECOLORS_CLR\" data-opt-name=\"color\" onchange=\"populateSelectedColor(id,'"+ChartType+"'LINECOLORS','"+mode+"')\" value=\"#1864ab\">")
		        .append("<div id=\""+ChartType+"LINECOLORS_CLR_DIV\" class=\"colorsSelectDiv\"></div>").append("</li>")
		        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+ChartType+"WIDTH\" data-key-type=\"data\">")
		        .append("<label>Line Width</label>")
		        .append("<input type=\"number\" id=\""+ChartType+"LINEWIDTH\" data-opt-name=\"width\" data-man=\"O\" title=\"\"/>")
		        .append("</li>")
		        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+ChartType+"DASH\" data-key-type=\"data\">")
		        .append("<label>Line Dash</label>")
		        .append("<select name=\"text-position\" id=\""+ChartType+"LINEDASH\" data-opt-name=\"dash\" data-man=\"O\" title=\"\">")
		        .append("<option value=\"solid\">Solid</option>").append("<option value=\"dotted\">Dotted</option>")
		        .append("<option value=\"smooth\">Curve</option>").append("<option value=\"dashed\">Dashed</option>")
		        .append("</select>").append("</li>")
		        .append("</ul>").append("</li>");
				return eChartProperties;
	}

	public StringBuilder getChartAreaProperties(String ChartType, String mode) {
		StringBuilder eChartProperties = new StringBuilder();
		eChartProperties.append("<li id=\"marker-filter\" data-column-name=\""+ChartType+"AREA\" data-key-type=\"data\">")
        .append("<div class=\"main-container\"><div class=\"filter-container\">")
        .append("<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>")
        .append("<p>Chart Area</p>").append("</div>").append(getToggleButton(ChartType, "")).append("</div>")
        .append("<ul class=\"sub-filters\" id=\""+ChartType+"\" data-opt-name=\"marker\" style=\"display: none;\">")
        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+ChartType+"COLORSAREA\" data-key-type=\"data\">")
        .append("<label>Area Color</label>").append("<input type=\"hidden\" id=\""+ChartType+"COLORSAREA\" value=\"#1864ab\">")
        .append("<input type=\"color\" id=\""+ChartType+"COLORSAREA_CLR\" data-opt-name=\"color\" onchange=\"populateSelectedColor(id,'"+ChartType+"'AREACOLOR','"+mode+"')\" value=\"#1864ab\">")
        .append("<div id=\""+ChartType+"COLORSAREA_CLR_DIV\" class=\"colorsSelectDiv\"></div>")
        .append("</li>")
        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+ChartType+"OPACITY\" data-key-type=\"data\">")
        .append("<label>Opacity</label>")
        .append("<input type=\"number\" placeholder=\"Value Between 0 to 1\"id=\""+ChartType+"OPACITY\" data-opt-name=\"size\" data-man=\"O\" title=\"\"/>")
        .append("</li>").append("</ul>").append("</li>");
		return eChartProperties;
	}

	public StringBuilder getEchartProperties(String ChartType) {

		StringBuilder eChartProperties = new StringBuilder();
		eChartProperties.append("<li class=\"general-filters active-filter\" data-column-name=\""+ChartType+"MODE\" data-key-type=\"data\">")
		        .append("<div class=\"sub-filterItems\"><label>Mode</label>")
		        .append("<select name=\"text-info\" id=\""+ChartType+"MODE\" data-opt-name=\"mode\">")
		        .append("<option value=\"lines\" selected>Lines</option>")
		        .append("<option value=\"lines+markers\">Lines and Markers</option>")
		        .append("</select>").append("</div>").append("</li>");

		eChartProperties.append("<li class=\"general-filters active-filter\" data-column-name=\"" + ChartType
				+ "LABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Data Visible</p>"
				+ "<select name=\"text-info\" id=\"" + ChartType + "LABELDATA\" data-opt-name=\"textinfo\">"
				+ "<option value=\"''\">None</option>" + "<option value=\"x\">Label</option>"
				+ "<option value=\"y\">Value</option>" + "<option value=\"%\">Percentage</option>"
				+ "<option value=\"x+y\">Label and value</option>"
				+ "<option value=\"x+%\">Label and Percentage</option>"
				+ "<option value=\"y+%\">Value and Percentage</option>" + "</select>" + "</div>" + "</li>"
				+ "<li class=\"general-filters active-filter\" data-column-name=\"" + ChartType
				+ "HOVERLABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">"
				+ "<p>Hover Data Visible</p>" + "<select name=\"text-info\" id=\"" + ChartType
				+ "HOVERLABELDATA\" data-opt-name=\"hoverinfo\" >" + "<option value=\"x\">Label</option>"
				+ "<option value=\"y\">Value</option>" + "<option value=\"%\">Percentage</option>"
				+ "<option value=\"x+y\" selected>Label and value</option>"
				+ "<option value=\"x+%\">Label and Percentage</option>"
				+ "<option value=\"y+%\">Value and Percentage</option>" + "</select>" + "</div>" + "</li>"
				+ "</li>" + "<li class=\"general-filters active-filter\" data-column-name=\"" + ChartType
				+ "LABELPOSITION\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Text Position</p>"
				+ "<select name=\"text-position\" id=\"" + ChartType + "LABELPOSITION\" data-opt-name=\"textposition\">"
				+ "<option value=\"inside\">Inside</option>" + "<option value=\"top\">Top</option>"
				+ "<option value=\"right\">Right</option>" + "<option value=\"bottom\">Bottom</option>"
				+"<option value=\"left\">Left</option>" 
				+ "</select>"
				+ "</div>" + "</li>"
				+"<li class=\"general-filters active-filter\" data-column-name=\"" + ChartType
				+ "MARKERSHAPE\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">" + "<p>Marker Shape</p>"
				+ "<select name=\"text-position\" id=\"" + ChartType + "MARKERSHAPE\" data-opt-name=\"textposition\">"
				+ "<option value=\"triangle\">Triangle</option>" + "<option value=\"square\">Square</option>"
				+ "<option value=\"circle\">Circle</option>" 
				+ "</select>"
				+ "</div>" + "</li>");
		eChartProperties.append(getChartHover(ChartType, "data"));
		
		eChartProperties.append("<li id=\"marker-filter\" data-column-name=\""+ChartType+"MARKER\" data-key-type=\"data\">")
		        .append("<div class=\"main-container\"><div class=\"filter-container\">")
		        .append("<img src=\"images/down-chevron.png\" alt=\"Down Chevron\" class=\"icons visualDarkMode\"/>")
		        .append("<p>Chart Markers</p>").append("</div>").append(getToggleButton(ChartType, "")).append("</div>")
		        .append("<ul class=\"sub-filters\" id=\""+ChartType+"MARKER\" data-opt-name=\"marker\" style=\"display: none;\">")
		        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+ChartType+"COLORSMARKER\" data-key-type=\"data\">")
		        .append("<label>Marker Color</label>").append("<input type=\"hidden\" id=\""+ChartType+"COLORSMARKER\" value=\"\">")
		        .append("<input type=\"color\" id=\""+ChartType+"COLORSMARKER_CLR\" data-opt-name=\"color\" onchange=\"populateSelectedColor(id,'"+ChartType+"'MARKERCOLOR','M')\" value=\"#1864ab\">")
		        .append("<div id=\""+ChartType+"COLORSMARKER_CLR_DIV\" class=\"colorsSelectDiv\"></div>")
		        .append("</li>")
		        .append("<li class=\"sub-filterItems active-filter\" data-column-name=\""+ChartType+"MARKERSIZE\" data-key-type=\"data\">")
		        .append("<label>Marker Size</label>")
		        .append("<input type=\"number\" id=\""+ChartType+"MARKERSIZE\" data-opt-name=\"size\" data-man=\"O\" title=\"\"/>")
		        .append("</li>").append("</ul>").append("</li>");
		
		

		
		

		return eChartProperties;
	}
	
	public JSONObject getVoiceResponse(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String inputText = (String) request.getParameter("inputText");
			String mainDiv = "";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> inputMap = new LinkedMultiValueMap(); 
			inputMap.add("query", inputText); 
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<MultiValueMap<String, String>>(inputMap,
					headers);
			RestTemplate template = new RestTemplate();
			ResponseEntity<JSONObject> response = template.postForEntity("http://apihub.pilogcloud.com:6652/voice_command_data/",
					entity, JSONObject.class);
			JSONObject apiDataObj = response.getBody(); 

			resultObj.put("result", apiDataObj);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	
	public JSONObject getLGFeaturesInfo(HttpServletRequest request) {
        String result = "";
        JSONArray featureArr = new JSONArray();
        JSONObject resultdata = new JSONObject();
        String priceStr = "";
        try {
            priceStr = "<div class='pricingCustomeTable'><table class='table table-hover table-bordered'><thead><tr class='active'> "
                    + "<th style=\"background:#fff\" class=\"firstChildClass\"><div id ='expandId' class='expandClass'>"
                    + "<img src='images/plus-solid.svg' width='16px' id='expandImageId' class='VisionImageVisualization' title='Show/Hide pane'/>"
                    + "<span class='expandClass'>Expand all</span></div></th>\r\n <th class='subscriptionClass'>"
                    
                    + "<center><h3 class=\"PlanTitle\">Basic</h3><p\r\n class=\"text-muted text-sm\">Ideal\r\n for\r\n small operations.</p>\r\n "
                    + "<h3 class=\"panel-title price\"><del style=\"color:red\">$299</del>"
                    + " <b style=\"font-weight:1000\">$149 </b> User/Month</h3>\r\n <a href='https://smart.integraldataanalytics.com/getIGinfo?Lgtype=Basic' target='_blank'>"
                    + "<button class=\"btn btn-primary\" id='btnclass'>Buy now</button></a>\r\n </center></th>\r\n <th class='subscriptionClass'>"
                    + "<center><h3 class=\"PlanTitle\">Professional</h3><p\r\n class=\"text-muted text-sm\">Perfect\r\n for\r\n larger operations.</p>\r\n "
                    + "<h3 class=\"panel-title price\"><del style=\"color:red\">$399</del> "
                    + "<b style=\"font-weight:1000\">$199 </b> User/Month</h3>\r\n <a href='https://smart.integraldataanalytics.com/getIGinfo?Lgtype=Professional' target='_blank'>"
                    + "<button class=\"btn btn-primary\">Buy now</button></a></center></th>\r\n <th class='subscriptionClass'>"
                    + "<center><h3 class=\"PlanTitle\">Enterprise</h3><p\r\n class=\"text-muted text-sm\">Enterprise\r\n<h3 class=\"panel-title price\">$$$</h3>"
                    + "<a href='https://smart.integraldataanalytics.com/getIGinfo?Lgtype=Enterprise' target='_blank'>"
                    + "<button class=\"btn btn-primary\" >Buy now</button></a></center></th>\r\n</tr>\r\n</thead>\r\n";
                List processGstList = dashBoardsDAO.getFeaturesPrice();
                if (!processGstList.isEmpty()) {
                    Clob clob = (Clob)processGstList.get(0);
//                    priceStr = piLogCloudUtills.(processGstList.get(0));
                    priceStr =  cloudUtills.clobToString(clob);
                } else {
                    priceStr = "<div class='pricingCustomeTable'><table class='table table-hover table-bordered'><thead><tr class='active'> <th style=\"background:#fff\" class=\"firstChildClass\"><div id ='expandId' class='expandClass'><img src='images/plus-solid.svg' width='16px' id='expandImageId' class='VisionImageVisualization' title='Show/Hide pane'/><span class='expandClass'>Expand all</span></div></th>\r\n <th class='subscriptionClass'><center><h3 class=\"PlanTitle\">Basic</h3><p\r\n class=\"text-muted text-sm\">Ideal\r\n for\r\n small operations.</p>\r\n <h3 class=\"panel-title price\"><del style=\"color:red\">$299</del> <b style=\"font-weight:1000\">$149 </b> User/Month</h3>\r\n <a href='https://smart.integraldataanalytics.com/getIGinfo?Lgtype=Basic' target='_blank'><button class=\"btn btn-primary\" id='btnclass'>Buy now</button></a>\r\n </center></th>\r\n <th class='subscriptionClass'><center><h3 class=\"PlanTitle\">Professional</h3><p\r\n class=\"text-muted text-sm\">Perfect\r\n for\r\n larger operations.</p>\r\n <h3 class=\"panel-title price\"><del style=\"color:red\">$399</del> <b style=\"font-weight:1000\">$199 </b> User/Month</h3>\r\n <a href='https://smart.integraldataanalytics.com/getIGinfo?Lgtype=Professional' target='_blank'><button class=\"btn btn-primary\">Buy now</button></a></center></th>\r\n <th class='subscriptionClass'><center><h3 class=\"PlanTitle\">Enterprise</h3><p\r\n class=\"text-muted text-sm\">Enterprise\r\n<h3 class=\"panel-title price\">$$$</h3>\r\n<a href='https://smart.integraldataanalytics.com/getIGinfo?Lgtype=Enterprise' target='_blank'><button class=\"btn btn-primary\" >Buy now</button></a></center></th>\r\n</tr>\r\n</thead>\r\n";
            }
            List resultlist = dashBoardsDAO.getLgFeature(request);
//      result = result + "<div class='pricingCustomeTable'><table class='table table-hover table-bordered'><thead><tr class='active'> <th style=\"background:#fff\" class=\"firstChildClass\"><div id ='expandId' class='expandClass'><img src='images/plus-solid.svg' width='16px' id='expandImageId' class='VisionImageVisualization' title='Show/Hide pane'/><span class='expandClass'>Expand all</span></div></th>\r\n <th class='subscriptionClass'><center><h3 class=\"PlanTitle\">Basic</h3><p\r\n class=\"text-muted text-sm\">Ideal\r\n for\r\n small operations.</p>\r\n <h3 class=\"panel-title price\">99 $ User/Month</h3>\r\n <button class=\"btn btn-primary\" id='btnclass' onclick=getform('Basic_Model')>Buy now</button>\r\n </center></th>\r\n <th class='subscriptionClass'><center><h3 class=\"PlanTitle\">Professional</h3><p\r\n class=\"text-muted text-sm\">Perfect\r\n for\r\n larger operations.</p>\r\n <h3 class=\"panel-title price\">199 $ User/Month</h3>\r\n <button class=\"btn btn-primary\"onclick=getform('Standard_Model')>Buy now</button></center></th>\r\n <th class='subscriptionClass'><center><h3 class=\"PlanTitle\">Enterprise Model</h3><p\r\n class=\"text-muted text-sm\">Enterprise\r\n<h3 class=\"panel-title price\">$$$</h3>\r\n<button class=\"btn btn-primary\" onclick=getform('Enterprise_Model')>Buy now</button></center></th>\r\n</tr>\r\n</thead>\r\n";
            result = result + priceStr;
            if ((resultlist != null) && (!resultlist.isEmpty())) {
                String featureName = "";

                result = result + "<tbody>";
                for (int i = 0; i < resultlist.size(); i++) {
                    Object[] rowData = (Object[]) resultlist.get(i);
                    featureName = (String) rowData[0];
                    String basicModel = (String) rowData[1];
                    String standeredModel = (String) rowData[2];
                    String EnterprisedModel = (String) rowData[3];
                    String icon = (String) rowData[6];
                    List featurelist = dashBoardsDAO.getSubFeature(request, featureName);
                    if (featurelist.size() > 0) {
                        if ((featureName.equalsIgnoreCase((String) rowData[0]))
                                && (!featureArr.contains(featureName))) {
                            result = result + "<tr data-positionNumber='1' data-parentFeature = '" + featureName.replaceAll(" ", "") + "' data-toggle='collapse' data-target='#accordion" + featureName.replaceAll(" ", "") + "' class='clickable collapse-row collapsed'>\r\n<td colspan=\"1\" align=\"left\"  text-align: left !important;\"\r\nclass=\"active\"><span class='rowtitle'>" + featureName + "</span><span id='imageid' class='imageClass'><img src='" + icon + "' width='50px' id='expandImageId' class='VisionImageVisualization'></span></td>";
                            if (basicModel.equalsIgnoreCase("Y")) {
                                result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                            } else if (basicModel.equalsIgnoreCase("N")) {
                                result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                            } else {
                                result = result + "<td>" + basicModel + "</td>\r\n";
                            }
                            if (standeredModel.equalsIgnoreCase("Y")) {
                                result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                            } else if (standeredModel.equalsIgnoreCase("N")) {
                                result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                            } else {
                                result = result + "<td>" + standeredModel + "</td>\r\n";
                            }
                            if (EnterprisedModel.equalsIgnoreCase("Y")) {
                                result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                            } else if (EnterprisedModel.equalsIgnoreCase("N")) {
                                result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                            } else {
                                result = result + "<td>" + EnterprisedModel + "</td>\r\n";
                            }
                            result = result + "</tr>\r\n";
                            featureArr.add(featureName);
                        }
                    } else if ((featureName.equalsIgnoreCase((String) rowData[0]))
                            && (!featureArr.contains(featureName))) {
                        result = result + "<tr  data-positionNumber='1' data-parentFeature = '" + featureName.replaceAll(" ", "") + "' class='clickable'>\r\n<td colspan=\"1\" align=\"left\"  text-align: left !important;\"\r\nclass=\"active\"><span class='rowtitle'>" + featureName + "</span><span id='imageid' class='imageClass'><img src='" + icon + "' width='50px' id='expandImageId' class='VisionImageVisualization'></span></td>";
                        if (basicModel.equalsIgnoreCase("Y")) {
                            result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                        } else if (basicModel.equalsIgnoreCase("N")) {
                            result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                        } else {
                            result = result + "<td>" + basicModel + "</td>\r\n";
                        }
                        if (standeredModel.equalsIgnoreCase("Y")) {
                            result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                        } else if (standeredModel.equalsIgnoreCase("N")) {
                            result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                        } else {
                            result = result + "<td>" + standeredModel + "</td>\r\n";
                        }
                        if (EnterprisedModel.equalsIgnoreCase("Y")) {
                            result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                        } else if (EnterprisedModel.equalsIgnoreCase("N")) {
                            result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                        } else {
                            result = result + "<td>" + EnterprisedModel + "</td>\r\n";
                        }
                        result = result + "</tr>\r\n";
                        featureArr.add(featureName);
                    }
                    if ((featurelist != null) && (!featurelist.isEmpty())) {
                        for (int j = 0; j < featurelist.size(); j++) {
                            Object[] featurerowData = (Object[]) featurelist.get(j);
                            String subfeturedata = (String) featurerowData[0];
                            String subbasicModel = (String) featurerowData[1];
                            String substanderedModel = (String) featurerowData[2];
                            String subEntereprice = (String) featurerowData[3];
                            String subicon = (String) featurerowData[4];
                            List subfeaturelist = dashBoardsDAO.getSubFeature(request, subfeturedata);

                            if (subfeaturelist.size() > 0) {
                                if (subicon != null && !"null".equalsIgnoreCase(subicon) && !"".equalsIgnoreCase(subicon)) {
                                    result = result + "<tr data-parentFeature = '" + featureName.replaceAll(" ", "") + "' id='accordion" + featureName.replaceAll(" ", "") + "' data-toggle='collapse' data-target='#accordion" + subfeturedata.replaceAll(" ", "") + "' class='clickable collapse-row collapsed'>\r\n<td colspan=\"1\" align=\"left\"  text-align: left !important;\"\r\nclass=\"active\"><span class='rowtitle'>" + subfeturedata + "</span><span id='imageid' class='imageClass'><img src='" + subicon + "' width='50px' id='expandImageId' class='VisionImageVisualization'></span></td>";
                                } else {
                                    result = result + "<tr data-parentFeature = '" + featureName.replaceAll(" ", "") + "' id='accordion" + featureName.replaceAll(" ", "") + "' data-toggle='collapse' data-target='#accordion" + subfeturedata.replaceAll(" ", "") + "' class='clickable collapse-row collapsed'>\r\n<td colspan=\"1\" align=\"left\"  text-align: left !important;\"\r\nclass=\"active\"><span class='rowtitle'>" + subfeturedata + "</span></td>";
                                }
                                if (basicModel.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (basicModel.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + basicModel + "</td>\r\n";
                                }
                                if (standeredModel.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (standeredModel.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + standeredModel + "</td>\r\n";
                                }
                                if (EnterprisedModel.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (EnterprisedModel.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + EnterprisedModel + "</td>\r\n";
                                }
                                result = result + "</tr>\r\n";
                            } else {
                                if (subicon != null && !"null".equalsIgnoreCase(subicon) && !"".equalsIgnoreCase(subicon)) {
//               result = result + "<tr id='accordion" + featureName.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'><span class='rowtitle'>" + subfeturedata + "</span><span id='imageid' class='imageClass'><img src='" + subicon + "' width='50px' id='expandImageId' class='VisionImageVisualization'></span></td>";
                                    result = result + "<tr data-parentFeature = '" + featureName.replaceAll(" ", "") + "' id='accordion" + featureName.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'><span>" + subfeturedata + "</span><span id='imageid' class='imageClass'><img src='" + subicon + "' width='50px' id='expandImageId' class='VisionImageVisualization'></span></td>";
                                } else {
                                    result = result + "<tr data-parentFeature = '" + featureName.replaceAll(" ", "") + "' id='accordion" + featureName.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'>" + subfeturedata + "</td>";
                                }

//               result = result + "<tr id='accordion" + featureName.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'>" + subfeturedata + "</td>";
                                if (subbasicModel.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (subbasicModel.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + subbasicModel + "</td>\r\n";
                                }
                                if (substanderedModel.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (substanderedModel.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + substanderedModel + "</td>\r\n";
                                }
                                if (subEntereprice.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (subEntereprice.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + subEntereprice + "</td>";
                                }
                                result = result + "</tr>";
                            }

                            for (int y = 0; y < subfeaturelist.size(); y++) {
                                Object[] subfeaturerowData = (Object[]) subfeaturelist.get(y);
                                String subsubfeturedata = (String) subfeaturerowData[0];
                                String subsubbasicModel = (String) subfeaturerowData[1];
                                String subsubstanderedModel = (String) subfeaturerowData[2];
                                String subsubEntereprice = (String) subfeaturerowData[3];
                                String subsubicon = (String) subfeaturerowData[4];

                                if (subicon != null && !"null".equalsIgnoreCase(subicon) && !"".equalsIgnoreCase(subicon)) {
//               result = result + "<tr id='accordion" + featureName.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'><span class='rowtitle'>" + subfeturedata + "</span><span id='imageid' class='imageClass'><img src='" + subicon + "' width='50px' id='expandImageId' class='VisionImageVisualization'></span></td>";
                                    result = result + "<tr data-parentFeature = '" + featureName.replaceAll(" ", "") + "' id='accordion" + subfeturedata.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'><span>" + subsubfeturedata + "</span><span id='imageid' class='imageClass'><img src='" + subsubicon + "' width='50px' id='expandImageId' class='VisionImageVisualization'></span></td>";
                                } else {
                                    result = result + "<tr data-parentFeature = '" + featureName.replaceAll(" ", "") + "' id='accordion" + subfeturedata.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'>" + subsubfeturedata + "</td>";
                                }

//               result = result + "<tr id='accordion" + featureName.replaceAll(" ", "") + "' class=\"collapse\"><td style='padding-left: 40px'>" + subfeturedata + "</td>";
                                if (subsubbasicModel.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (subsubbasicModel.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + subsubbasicModel + "</td>\r\n";
                                }
                                if (subsubstanderedModel.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (subsubstanderedModel.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + subsubstanderedModel + "</td>\r\n";
                                }
                                if (subsubEntereprice.equalsIgnoreCase("Y")) {
                                    result = result + "<td><i style=\"color:limegreen\" class=\"fa fa-check\nfa-lg\"></i></td>";
                                } else if (subsubEntereprice.equalsIgnoreCase("N")) {
                                    result = result + "<td><i style=\"color:red\" class=\"fa fa-times fa-lg\" aria-hidden=\"true\"></i></td>";
                                } else {
                                    result = result + "<td>" + subsubEntereprice + "</td>";
                                }
                                result = result + "</tr>";

                            }

                        }
                    }
                }
                result = result + "</tbody>";
            }
            result = result + "</table>\r\n</div>";

            System.out.println("resultstr::::::::::::::::::" + result);
            resultdata.put("result", result);
            resultdata.put("featureArr", featureArr);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultdata;
    }
	
	
	public StringBuilder getinfo(HttpServletRequest request) {
        StringBuilder result = new StringBuilder();
        StringBuilder sideLgfeatures = new StringBuilder();
        try {
            JSONObject resultobj = new JSONObject();
            String tittle = request.getParameter("Lgtype");
            sideLgfeatures = dashBoardsDAO.getInfoBasicPlan(request, tittle);
            result.append("<div class=\"pageContainer\"><div class=\"containerWrapper\"><div class=\"leftSideContainer\">") //done
                    .append("<div class=\"logoHeader\"><img src=\"https://www.piloggroup.com/img/header/logo-header.png\" alt=\"&^^&\"></div>");
            result.append(sideLgfeatures);
            result.append("</div>");
            result.append("<div class=\"RightContainer\">")
                    .append("<div class=\"progress-container\" id=\"progressContainer\"><div class=\"step-progress\"><div class=\"step-wrapper\" id=\"first-wrapper-progress\"><div class=\"step-icon step-contactDetailsImg \"></div><div class=\"step-name\">Contact Info</div></div><div id=\"second-wrapper-progress\" class=\"step-wrapper\"><div class=\"step-icon billingInfo-progress\"></div><div class=\"step-name\">Billing Info</div></div><div id=\"third-wrapper-progress\" class=\"step-wrapper \"><div class=\"step-icon checkout-progress\"></div><div class=\"step-name\">Checkout</div></div></div></div>")
                    .append("<div class=\"formWrapper\" id=\"paymentDirectBodyFormPage\">");
            List resultList = dashBoardsDAO.getInfoDynamicHTML(request);
            //som
            int n = 0;
            int d = 0;
            StringBuilder firstDiv = new StringBuilder();
            StringBuilder secondDiv = new StringBuilder();
            StringBuilder thirdDiv = new StringBuilder();
            firstDiv.append("<div class=\"rightContainerFirstMainDiv\"><div class=\"rightContainerMainspan\">Personal Details:</div><div class=\"rightContainerFirstDiv firstRow\" id=\"rightContainerFirstDiv\">");
            secondDiv.append("<div class=\"rightContainerSecondMainDiv\"><div class=\"rightContainerMainspan\">Company Details:</div><div class=\"rightContainerSecondDiv\" id=\"rightContainerSecondDiv\">");
            //thirdDiv.append("<div class=\"rightContainerthirdMainDiv\"><div class=\"rightContainerMainspan\">Doman Details:</div><div class=\"rightContainerthirdDiv\" id=\"rightContainerthirdDiv\">");
            if (resultList != null && !resultList.isEmpty()) {
            	for (int i = 0; i < resultList.size(); i++) {
                    Object[] rowData = (Object[]) resultList.get(i);
                    if (rowData[5].toString().equalsIgnoreCase("F")) {                   
                        StringBuilder resultStr = textFildSetRow(request, rowData);
                        firstDiv.append(resultStr);
                    } else if (rowData[5].toString().equalsIgnoreCase("S")) {
                        n = n + 1;
                        d = n % 2;
                        StringBuilder resultStr = new StringBuilder();
                        String fieldType = (String) rowData[2];
                        if (!fieldType.equalsIgnoreCase("H")) {
                            if (n == 1) {
                                secondDiv.append("<div class=\"firstRow\">");
                                resultStr = textFildSetRow(request, rowData);
                                secondDiv.append(resultStr);
                            } else if (d == 0) {
                                resultStr = textFildSetRow(request, rowData);
                                secondDiv.append(resultStr);
                                secondDiv.append("</div>");
                            } else {
                                secondDiv.append("<div class=\"firstRow\">");
                                resultStr = textFildSetRow(request, rowData);
                                secondDiv.append(resultStr);
                            }
                        } else {
                            resultStr = textFildSetRow(request, rowData);
                            secondDiv.append(resultStr);
                        }
                    } else if (rowData[5].toString().equalsIgnoreCase("T")) {
                        StringBuilder resultStr = textFildSetRow(request, rowData);
                        //thirdDiv.append(resultStr);
                    }

                }
                firstDiv.append("</div><div class=\"rightContainerFirstErrorDiv\" id=\"rightContainerFirstErrorDiv\" style=\"display:none\"></div></div>");
                result.append(firstDiv);
                secondDiv.append("</div></div><div class=\"rightContainerSecondErrorDiv\" id=\"rightContainerSecondErrorDiv\" style=\"display:none\"></div></div>");
                result.append(secondDiv);
                //thirdDiv.append("</div><div class=\"rightContainerthirdErrorDiv\" id=\"rightContainerthirdErrorDiv\" style=\"display:none\"></div></div>");
                //result.append(thirdDiv);
                result.append("</div><div class=\"firstRow downBtnField\"><div class=\"checkboxLevelCLS\"><input type=\"checkbox\" onclick=\"checkFormValidation()\" name=\"\" id=\"paymentFormCheck\" required><span class=\"iAgreeCls\">I agree the</span><a href=\"javascript:void(0)\" style=\"color:blue;margin-top:3px;\"> Terms & Conditions </a></div>");
                result.append("<div class=\"checkoutBtns\" onclick=\"getNextFeatureInfo('" + tittle + "')\" class=\"nextbtn\" id=\"paymentFormNextPage\"><div>Next</div>");
                result.append("</div></div>");
                
            }
            result.append("</div></div></div>");     
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
	
	private StringBuilder textFildSetRow(HttpServletRequest request, Object[] rowData) {
        StringBuilder result = new StringBuilder();
        try {
            String fieldType = (String) rowData[2];
            String colName = (String) rowData[0];
            String colLevel = (String) rowData[1];
            String condFlag = (String) rowData[3];
            String attrValue = (String) rowData[4];
            String title = request.getParameter("Lgtype");
            JSONObject resultobj = dashBoardsDAO.getCountryList(request);
            List countryList = (List) resultobj.get("dataList");
            StringBuilder cuntryOpation = new StringBuilder();
            StringBuilder cityOpation = new StringBuilder();
//som
            for (int i = 0; i < countryList.size(); i++) {
                Object[] cunData = (Object[]) countryList.get(i);
                String CountryCode = (String) cunData[0];
                String countryname = (String) cunData[1];
                cuntryOpation.append("<option value=\'" + countryname + "\' data-dialcode=\'" + CountryCode + "\' >" + countryname + "</option>");
            }

            StringBuilder currencyLStr = new StringBuilder();
            currencyLStr.append("<option>INR </option><option>USD</option><option>EURO</option>");
//son
            String btnAttr = "";
            if (rowData[4] != null) {
                btnAttr = attrValue.replace("&&", "('" + title + "')");
            }
            if (fieldType.equalsIgnoreCase("T")) {
                if ("PHONE_NUMBER".equalsIgnoreCase(colName)) {
                    result.append("<div class=\"textLevelCLS\"><label for=\"\">Phone Number</label>")
                            .append("<div class=\"PhoneNumberInputs\">")
                            .append("<input type=\"text\" class=\"CountryCode\" id=\"dialCode\" >")
                            .append("<input type=\"tel\"  pattern=\"^\\d{10}$\" class=\"inputPayment CountryNumber\" name=\"billing_tel\" id=\"CONTACT_PHONE_NO\" required></div></div>");
                } else if ("EMAIL".equalsIgnoreCase(colName)) {
                    result.append("<div class=\"textLevelCLS customDiv\" id=\"paymentMailFeild\"><div class=\"EmailFeildForm\" id=\"EmailFeildForm\"><label for=\"\">Email</label><input data-attr=false name=\"billing_email\" pattern=\"^[_a-zA-Z0-9-]+(\\\\.[_a-zA-Z0-9-]+)*@[a-z0-9-]+(\\\\.[a-z0-9-]+)*(\\\\.[a-z]{2,4})$\\\" class=\"inputPayment\" id=\"CONTACT_MAIL_ID\" type=\"text\"></div>")
                            .append("<button id=\"EmailFeildBTN\" onclick=\"getPaymentEmailOtp()\">OTP</button><div style=\"display:none\" id=\"otpVerificationContaniner\" class=\"otpVerificationContaniner\"></div></div>");
                } else {
                    result.append("<div class=\"textLevelCLS\"><label for=\"\">" + colLevel + "</label><input name=\"billing_" + colLevel.toLowerCase() + "\" " + attrValue + "></div>");
                }
            } else if (fieldType.equalsIgnoreCase("L")) {
                if (colLevel.toUpperCase().equalsIgnoreCase("CURRENCY")) {
                    result.append("<div class=\"listLevelCLS\"><label for=\"\">" + colLevel + "</label>")
                            .append("<select id=\"CUSTOMER_" + colLevel.toUpperCase() + "\" name=\"" + colLevel.toLowerCase() + "\" " + attrValue + ">")
                            .append("<option value=\"\" >Select " + colLevel + " </option>");
                    result.append(currencyLStr);
                    result.append("</select></div>");
                } else {
                    result.append("<div class=\"listLevelCLS\"><label for=\"\">" + colLevel + "</label>")
                            .append("<select id=\"CUSTOMER_" + colLevel.toUpperCase() + "\" name=\"billing_" + colLevel.toLowerCase() + "\" " + attrValue + ">")
                            .append("<option value=\"\" >Select " + colLevel + " </option>");
                    if (colLevel.toUpperCase().equalsIgnoreCase("COUNTRY")) {
                        result.append(cuntryOpation);
                    }
                    result.append("</select></div>");
                }
            } else if (fieldType.equalsIgnoreCase("CL")) {
                result.append("<div class=\"checkListLevelCLS\"><label for=\"\" class=\"LablesStyle\">" + colLevel + "</label><div id=\"jqx" + colLevel.toLowerCase() + "\" type=\"text\"></div></div>");
            } else if (fieldType.equalsIgnoreCase("B")) {
                result.append("<div class=\"checkoutBtns\"><div " + btnAttr + ">Next</div>");
            } else if (fieldType.equalsIgnoreCase("C")) {
                result.append("<div class=\"checkboxLevelCLS\"><input type=\"checkbox\" onclick=\"checkFormValidation()\" name=\"\" id=\"paymentFormCheck\" required>I agree to the<a href=\"javascript:void(0)\" style=\"color:blue;margin-top:3px;\"> Terms & Conditions </a></div>");
            } else if (fieldType.equalsIgnoreCase("H")) {
                int randomNumber = ThreadLocalRandom.current().nextInt();
                String hiddAttr = "";
                if (rowData[4] != null) {
                    hiddAttr = attrValue.replace("&&&", "P-" + randomNumber + "");
                }
                result.append("<input " + hiddAttr + "/>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
	
	 public String getstate(HttpServletRequest request) {
	        return dashBoardsDAO.getstate(request);
	    }
	 
	 public JSONObject getCity(HttpServletRequest request) {
	        JSONObject resultObj = new JSONObject();
	        try {
	            StringBuilder result = new StringBuilder();
	            result.append("<option value=\"\">Select City</option>");
	            List resultList = dashBoardsDAO.getCity(request);
	            if (resultList != null && !resultList.isEmpty()) {
	                for (int i = 0; i < resultList.size(); i++) {
	                    String CityList = (String) resultList.get(i);
	                    result.append("<option >" + CityList + "</option>");
	                }
	            }
	            result.append("<option>Other</option>");
	            resultObj.put("City", result);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	 
	 public JSONObject addOnpackage(HttpServletRequest request) {
	        return dashBoardsDAO.addOnpackage(request);
	    }
	 
	 public JSONObject getverificationcode(HttpServletRequest request) {
	        JSONObject resultobj = new JSONObject();
	        try {
	            String otpnum = dashBoardsDAO.getverificationcode(request);
	            if (otpnum.equalsIgnoreCase("Success")) {
	                StringBuilder str = new StringBuilder();
	                str.append(" <div class='otpVerifyContainer' id='otpVerfication'>")
	                        .append("<div class='otpEnterDiv'><input name='OTP' id=\"paymentOTPFeild\" placeholder='Enter OTP'></div>")
	                        .append("<div class='otpVerify' onclick=\"getVerifyOTP()\">Verify</div>")
	                        .append("<div class='RevertbacktEmailDiv' onclick='hideEmailShowOtp()'>Email</div></div>");
	                resultobj.put("str", str);
	                resultobj.put("status", true);
	            } else {
	                StringBuilder str = new StringBuilder();
	                str.append("<i class=\"fa fa-times\" id=\"dataVerfiedMatch\" data-match=false aria-hidden=\"true\"></i>");
	                resultobj.put("str", str);
	                resultobj.put("status", false);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultobj;
	    }
	 
	 public JSONObject getOTPVerificationcode(HttpServletRequest request) {
	        JSONObject resultObj = new JSONObject();
	        try {
	            String encoderOtp = (String) request.getSession(false).getAttribute("PaymentVerifyOTP");
	            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encoderOtp);
	            String otp = new String(decodedBytes);
	            String val = (String) request.getParameter("val");
	            Boolean match;
	            if (val.matches(otp)) {
	                match = true;
	                resultObj.put("str", "<i class=\"fa fa-check\" id=\"dataVerfiedMatch\" data-match=" + match + " aria-hidden=\"true\"></i>");
	            } else {
	                match = false;
	                resultObj.put("str", "<i class=\"fa fa-times\" id=\"dataVerfiedMatch\" data-match=" + match + " aria-hidden=\"true\"></i>");
	            }

	            resultObj.put("match", match);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	 
	 
	 public JSONObject getApplyDiscountCode(HttpServletRequest request) {
	        JSONObject resultObj = new JSONObject();
	        try {
	             resultObj = dashBoardsDAO.getApplyDiscountCode(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	 
	 public String getCcAvenueResponsePageUrl(HttpServletRequest request) {

//       StringBuilder responsePageUrl = new StringBuilder("https://secure.ccavenue.com/transaction.do?");
        StringBuilder responsePageUrl = new StringBuilder();
        StringBuilder ccaRequest = new StringBuilder();
       JSONObject textParam = new JSONObject();
                   Enumeration enumeration = request.getParameterNames();
                   while (enumeration.hasMoreElements()) {
                       String name = enumeration.nextElement().toString();
                       String val = (String) request.getParameter(name);
                       textParam.put(name, val);
                   }
        
         
           
                   
   
       
       try {
        String currency = (String) textParam.get("currency"); 
        AesCryptUtil aesUtil = new AesCryptUtil();
        
         if (currency != null && !"".equalsIgnoreCase(currency) && !"null".equalsIgnoreCase(currency) && "INR".equalsIgnoreCase(currency))
         {
        responsePageUrl = new StringBuilder("https://test.ccavenue.com/transaction.do?"); 
        //responsePageUrl = new StringBuilder("https://secure.ccavenue.ae/transaction.do?"); 
         responsePageUrl.append("command=initiateTransaction")
               .append("&merchant_id=2165579")
               .append("&access_code=AVQQ29KC84BH82QQHB")
               .append("&encRequest=");   
         
         aesUtil = new AesCryptUtil(CCAVENUE_WORKING_KEY_INR);
         }else{
         responsePageUrl = new StringBuilder("https://secure.ccavenue.ae/transaction/transaction.do?");    
         responsePageUrl.append("command=initiateTransaction")
               .append("&merchant_id=0045990")
               .append("&access_code=AVAU04KJ78AL98UALA")
               .append("&encRequest=");
         aesUtil = new AesCryptUtil(CCAVENUE_WORKING_KEY_NON_INR);
         }   
           
           if (aesUtil == null) {
               return "";
           }
           StringBuilder pname = new StringBuilder();
           String pvalue = "";
			/*
			 * while (enumeration.hasMoreElements()) { String enxt =
			 * enumeration.nextElement().toString(); pname.setLength(0); pname.append(enxt);
			 * pvalue = request.getParameter(pname.toString()); if
			 * ("tid".equalsIgnoreCase(pname.toString()) && "".equalsIgnoreCase(pvalue)) {
			 * pvalue = String.valueOf(System.currentTimeMillis() / 1000);// for generating
			 * unique tranaction IDs } if (!pname.toString().equalsIgnoreCase("addON")) {
			 * ccaRequest.append(pname) .append("=") .append(pvalue) .append("&"); } }
			 */
           
           textParam.keySet().forEach(keyStr
                   -> {
               String keyvalue = (String) textParam.get(keyStr);
               
               System.out.println("key: " + keyStr + " value: " + keyvalue);
               if ("tid".equalsIgnoreCase(keyStr.toString()) && "".equalsIgnoreCase(keyvalue)) {
                   keyvalue = String.valueOf(System.currentTimeMillis() / 1000);
               }
               
               if (!keyStr.toString().equalsIgnoreCase("addON")) {
                 ccaRequest.append(keyStr)
                   .append("=")
                   .append(keyvalue)
                   .append("&");
               }
               
           });
           String url = retUrl(request);
           System.out.println("redirect_url:" + url);
           ccaRequest.append("redirect_url")
                   .append("=")
                   .append(url)
                   .append("&");
           ccaRequest.append("cancel_url")
                   .append("=")
                   .append("https://smart.integraldataanalytics.com/getIGinfo?Lgtype=Basic")
                   .append("&");
//           StringBuilder ccaRequest1 = new StringBuilder();   
//           ccaRequest1.append("ERP=SmartBI&totalOrginalDisAmount=750000&billing_address=india&language=EN&merchant_id=2165579&billing_lastname=singh&integration_type=iframe_normal&disCouponPercentage=0&billing_tel=7007089689&disCouponAmmount=0&billing_name=som&billing_state=Assam&requestId=MDc3MUY3QzUxNEZBQkZBMUUwNjMwNDAwMDMwQUY3Nzc=&billing_email=jagadish.kumar@piloggroup.com&billing_zip=7845562&currency=INR&tittle=Basic&amount=750000&billing_country=India&billing_city=Chapar&billing_company=pilog&discountCode=&Domain=SmartBI&order_id=P--1324392233&totalOrginalAmount=750000&redirect_url=http://localhost:8080/integral/setInfo?status=MDc3MUY3QzUxNEZBQkZBMUUwNjMwNDAwMDMwQUY3Nzc=&cancel_url=http://localhost:8080/integral/getIGinfo?Lgtype=Basic&");
           StringBuilder encRequest = new StringBuilder(aesUtil.encrypt(ccaRequest.toString()));
           if (encRequest == null || "".equalsIgnoreCase(encRequest.toString())) {
               return ""; 
           }
           
           responsePageUrl.append(encRequest);
       } catch (Exception e) {
           e.printStackTrace();
       }
       return responsePageUrl.toString();
   }
	 
	 public String retUrl(HttpServletRequest request) {
	        String result = "";
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
	            String countryStr = (String) textParam.get("billing_country");
	            String titleStr = (String) textParam.get("tittle");
	            String country = countryStr.substring(0, 2);
	            DateTimeFormatter dateFormate = DateTimeFormatter.ofPattern("ddMMyyyyhh");
	            LocalDateTime toDate = LocalDateTime.now();
	            String todayDate = dateFormate.format(toDate);
	            textParam.put("invoice", " P" + country.toUpperCase() + titleStr.toUpperCase() + todayDate);
	            String orgn_id = (String) dashBoardsDAO.saveTransactionDetailsDB(request, textParam, text);
	            String encodedString = java.util.Base64.getEncoder().encodeToString(orgn_id.getBytes());
	            result = "https://smart.integraldataanalytics.com/setIGInfo?status=" + encodedString + "";
	            //result = "https://idxp1.pilogcloud.com/iVisionDXP/setInfo?status=" + encodedString + "";
	            //result = "http://localhost:8080/integral/setIGInfo?status=" + encodedString + "";
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	 
	 public String setInfo(HttpServletRequest request) {
	        String result = "";
	        try {
	            JSONObject textParam = dashBoardsDAO.getDBAllTranstionDetails(request);
	            String subject = "";
	            String column = "";
	            String textPdf = getPdfStr(request, textParam);
	            String email = (String) textParam.get("billing_email");
	            String status = (String) request.getParameter("status");
	            byte[] decodedBytes = java.util.Base64.getDecoder().decode(status);
	            String decodedString = new String(decodedBytes);
	            
	            String orgn_id = textParam.get("requestId").toString();
	            String encodedString = java.util.Base64.getEncoder().encodeToString(orgn_id.getBytes());
	            textParam.put("requestId", encodedString);
	            
	            //String successEmail = dashBoardsDAO.sendEmailText(request, textPdf, subject, email, column, textParam);
//	            String status = (String) request.getParameter("status");
	            StringBuilder fieldresult = new StringBuilder();
	            fieldresult.append("<div class=\"paymentSuccessfulMaiWrapper\">")
	                    .append("<div class=\"paymentSuccessfulPageInnerMainDiv\">")
	                    .append("<div class=\"showStatusImgDiv\">")
	                    .append("<span class=\"paymentImgClass\"><img src=\"images/successFull.gif\" alt=\"\"></span>")
	                    .append("<span class=\"paymentTextClass\"><h2>Transaction Successful</h2></span></div> ")
	                    .append("<div class=\"paymentSucc_Discription_Div\">")
	                    .append("<div class=\"paymentStatusDis_Div\">")
	                    .append("<span class=\"greetClass\"><h4>Congratulations your subscription process is completed. </h4></span>")
	                    .append("<span><h6>Your Order No: " + textParam.get("order_id") + ". Please check your mail for payment recepit.</h6></span>")
	                    .append("<span><h6>Kindly note shortly you will recieve an email with Admin Login Credentials to configure the iMDRM LG system for your Organisation</h6></span>")
	                    .append("<span class=\"paymentButtonClass\"></span>")
	                    .append("</div></div></div></div></div></div>");
	            String successDataBaseAdd = "";
	            String text = "";
	            request.getSession().setAttribute("flag", "U");
	            textParam.put("PAYMENT_STATUS","SUCCESS");
	            textParam.put("ORGN_ASSIGNED_STATUS","PENDING");
	            text = "PayementCompleted";
	            successDataBaseAdd = dashBoardsDAO.saveTransactionDetailsDB(request, textParam, text);
	            if (successDataBaseAdd.contentEquals(decodedString)) {
	                String title = textParam.get("tittle").toString();
	                StringBuilder lastStr = getSideLastPageData(request, fieldresult, title);
	                result = lastStr.toString();
	                String orgnId = dashBoardsDAO.createOrgn(request,textParam);
	                RegistrationDTO registrationDTO = setAllToRegistrationDTO(request, textParam);
	                textParam.put("orgnId", orgnId);
	                dashBoardsDAO.updateNoofUsersForNewSubscription(request,textParam);
	                JSONObject resultObj = registrationService.registerUser(registrationDTO, request);
	                if(resultObj !=null && !resultObj.isEmpty() && 
	                		resultObj.get("Message") !=null && 
	                		!"".equalsIgnoreCase(String.valueOf(resultObj.get("Message")))
	                		&& String.valueOf(resultObj.get("Message")).contains("Registration Successfully Completed."))
	                {
	                	String userName = (String) resultObj.get("userName");
	                	textParam.put("userName", userName);
	                	String successEmail = dashBoardsDAO.sendEmailText(request, textPdf, subject, email, column, textParam);
	                	dashBoardsDAO.updatePasswordParamFlagForNewSubscriptedUsers(request,textParam);
	                }
	            } else {
	                result = "<div class=\"errorMsg\">Payment Recipt failed<div>" + fieldresult + "";
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	 
	 public StringBuilder getSideLastPageData(HttpServletRequest request, StringBuilder text, String tittle) {
	        StringBuilder result = new StringBuilder();
	        try {
	            StringBuilder sideLgfeatures = new StringBuilder();
	            JSONObject resultobj = new JSONObject();
	            sideLgfeatures = dashBoardsDAO.getInfoBasicPlan(request, tittle);
	            result.append("<div class=\"pageContainer\"><div class=\"containerWrapper\"><div class=\"leftSideContainer\">") //done
	                    .append("<div class=\"logoHeader\"><img src=\"https://www.piloggroup.com/img/header/logo-header.png\" alt=\"&^^&\"></div>");
	            result.append(sideLgfeatures);
	            result.append("</div>");
	            result.append("<div class=\"RightContainer\">")
	                    .append("<div class=\"progress-container\" id=\"progressContainer\"><div class=\"step-progress\"><div class=\"step-wrapper step-active\"\" id=\"first-wrapper-progress\"><div class=\"step-icon step-contactDetailsImg step-active\"></div><div class=\"step-name\">Contact Info</div></div><div id=\"second-wrapper-progress\" class=\"step-wrapper step-active\"\"><div class=\"step-icon billingInfo-progress step-active\"></div><div class=\"step-name\">Billing Info</div></div><div id=\"third-wrapper-progress\" class=\"step-wrapper step-active\"\"><div class=\"step-icon checkout-progress step-active\"></div><div class=\"step-name\">Checkout</div></div></div></div>")
	                    .append("<div class=\"formWrapper\" id=\"paymentDirectBodyFormPage\">")
	                    .append(text);
	            result.append("</div></div></div>");
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	 
	 public String getPdfStr(HttpServletRequest request, JSONObject textParam) {
	        String result = "";
	        try {
	            DateTimeFormatter dateFormate = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
	            Date date = new Date();
	            SimpleDateFormat localDateFormate = new SimpleDateFormat("dd MMM yyyy hh:mm:ss");
	            LocalDateTime toDate = LocalDateTime.now();
	            String amountNum = (String) textParam.get("amount");
	            String amountStr = convert(parseInt(amountNum));
	            String strDate = formatter.format(date);
	            result = "<div style=\"border: 1px solid #000; width: 100%;\">"
	                    + "<table>"
	                    + "<tr>"
	                    + "<td  style=\" width: 50%;\">"
	                    + "<img width=\"100\" src=\"https://pilogcloud.com/iVisionDXP/images/PiLog_Logo_New.png\" alt=\"...\"/></td>"
	                    + "<td style=\"float: right; margin-left: 925px;\"><b>Tax Invoice/Bill of Supply/Cash Memo</b><br/>"
	                    + "(Original for Recipient)"
	                    + "</td>"
	                    + "</tr>"
	                    + "</table>"
	                    + "<table style=\"margin-top:10px; margin-bottom:10px; padding: 10px; border-bottom: 2px solid #ddd; width: 100%;\">"
	                    + "<tr>"
	                    + "<td style=\" width: 69%;\">PiLog Cloud Services<br/>"
	                    + "Date: " + localDateFormate.format(date) + ""
	                    + "</td></tr>"
	                    + "</table>"
	                    + "<table style=\"margin-top:10px; margin-bottom:10px; padding: 10px; border-bottom: 2px solid #ddd; width: 100%;\">"
	                    + "<tbody>"
	                    + "<tr>"
	                    //                    + "<td style=\"width: 56%;\"><b>Sold By:<br/> </b>PiLog India Private Limited<br/>"
	                    //                    + "MJR Magnifique, Rai Durg,<br/>"
	                    //                    + "X roads, Nanakaramguda,/1<br/>"
	                    //                    + "Hyderabad,Telangana-500008.<br/>"
	                    //                    + "IN</td>"
	                    + "<td style=\"width: 56%; display:block;\"><b>Billing Address:<br/></b>"
	                    + "" + textParam.get("billing_name") + "<br/>" + textParam.get("ORGN_NAME") + "<br/>"
	                    + "" + textParam.get("billing_address") + ",<br/>"
	                    + "" + textParam.get("billing_city") + ", "
	                    + "" + textParam.get("billing_state") + ",<br/> " + textParam.get("billing_zip") + ", "
	                    + "" + textParam.get("billing_country") + "</td>"
	                    //sold by remove 
	                    + "<td style=\"display:block;\"></td>"
	                    //sold by remove 
	                    + "</tr>"
	                    + "</tbody>"
	                    + "</table>"
	                    + "<table style=\"margin-top:10px; margin-bottom:10px;\">"
	                    + "<tbody>"
	                    + "<tr>"
	                    + "<td style=\" width: 49%;\">"
	                    + "<b>Order Number: </b>" + textParam.get("order_id") + "<br/>"
	                    + "<b>Order Date: </b>" + localDateFormate.format(date) + "<br/>"
	                    + "</td>"
	                    + "<td>"
	                    + "<b>Invoice Number: </b>" + textParam.get("invoice") + "<br/>"
	                    + "<b>Invoice Details: </b>IG(" + textParam.get("tittle") + ") <br/>"
	                    + "<b>Invoice Date: </b>" + strDate + "<br/>"
	                    + "</td>"
	                    + "</tr>"
	                    + "</tbody>"
	                    + "</table>"
	                    + "<table border=\"1\" style=\"border-collapse: collapse; width: 100%; margin-top:15px; margin-bottom:10px;\">"
	                    + "<thead>"
	                    + "<tr>"
	                    + "<th style=\"background-color: #f1f1f1; width: 10%;\">#</th>"
	                    + "<th colspan=\"2\" style=\"background-color: #f1f1f1; width: 15%;\">Description</th>"
	                    + "<th style=\"background-color: #f1f1f1;\">Unit Price</th>"
	                    + "<th style=\"background-color: #f1f1f1; width: 10%;\">Qty</th>"
	                    + "<th style=\"background-color: #f1f1f1;\">Net Amount</th>"
	                    //                    + "<th style=\"background-color: #f1f1f1;\">Tax Rate</th>"
	                    + "<th style=\"background-color: #f1f1f1; width: 10%;\">Tax Type</th>"
	                    + "<th style=\"background-color: #f1f1f1;\">Tax Amount</th>"
	                    + "<th style=\"background-color: #f1f1f1;\">Total Amount</th>"
	                    + "</tr>"
	                    + "</thead>"
	                    + "<tr>"
	                    + "<td >1</td>"
	                    + "<td colspan=\"2\">" + textParam.get("tittle") + "</td>"
	                    + "<td>" + textParam.get("amount") + "</td>"
	                    + "<td>1</td>"
	                    + "<td>" + textParam.get("amount") + "</td>"
	                    //                    + "<td>18%</td>"
	                    + "<td>Tax</td>"
	                    + "<td>" + textParam.get("amount") + "</td>"
	                    + "<td>" + textParam.get("amount") + "</td>"
	                    + "</tr>"
	                    + "<tr>"
	                    + "<td colspan=\"7\"><b>TOTAL:</b></td>"
	                    + "<td style=\"background-color: #f1f1f1;\">" + textParam.get("amount") + "</td>"
	                    + "<td style=\"background-color: #f1f1f1;\">" + textParam.get("amount") + "</td>"
	                    + "</tr>"
	                    + "<tr>"
	                    + "<td colspan=\"9\" style=\"text-align: inherit; padding: 10px 1px;\">"
	                    + "<b>Amount in Words:</b>"
	                    + "" + amountStr.toUpperCase() + " Rupees"
	                    + "</td>"
	                    + "</tr>"
	                    + "<tr>"
	                    + "<td colspan=\"9\" style=\"text-align: right; padding: 10px 25px;\">"
	                    + "<b>PiLog Cloud Services</b>"
	                    + "</td>"
	                    + "</tr>"
	                    + "</table>"
	                    + "<table>"
	                    + "<tr><td style=\"width:65%\"></td>"
	                    + "<td><small>PiLog Cloud Services<sup>TM</sup></small></td>"
	                    + "</tr>"
	                    + "</table>"
	                    + "</div>";
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	 
	 public static String convert(long number) {
	        // 0 to 999 999 999 999
	        if (number == 0) {
	            return "zero";
	        }

	        String snumber = Long.toString(number);

	        // pad with "0"
	        String mask = "000000000000";
	        DecimalFormat df = new DecimalFormat(mask);
	        snumber = df.format(number);

	        // XXXnnnnnnnnn
	        int billions = Integer.parseInt(snumber.substring(0, 3));
	        // nnnXXXnnnnnn
	        int millions = Integer.parseInt(snumber.substring(3, 6));
	        // nnnnnnXXXnnn
	        int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
	        // nnnnnnnnnXXX
	        int thousands = Integer.parseInt(snumber.substring(9, 12));

	        String tradBillions;
	        switch (billions) {
	            case 0:
	                tradBillions = "";
	                break;
	            case 1:
	                tradBillions = convertLessThanOneThousand(billions)
	                        + " billion ";
	                break;
	            default:
	                tradBillions = convertLessThanOneThousand(billions)
	                        + " billion ";
	        }
	        String result = tradBillions;

	        String tradMillions;
	        switch (millions) {
	            case 0:
	                tradMillions = "";
	                break;
	            case 1:
	                tradMillions = convertLessThanOneThousand(millions)
	                        + " million ";
	                break;
	            default:
	                tradMillions = convertLessThanOneThousand(millions)
	                        + " million ";
	        }
	        result = result + tradMillions;

	        String tradHundredThousands;
	        switch (hundredThousands) {
	            case 0:
	                tradHundredThousands = "";
	                break;
	            case 1:
	                tradHundredThousands = "one thousand ";
	                break;
	            default:
	                tradHundredThousands = convertLessThanOneThousand(hundredThousands)
	                        + " thousand ";
	        }
	        result = result + tradHundredThousands;

	        String tradThousand;
	        tradThousand = convertLessThanOneThousand(thousands);
	        result = result + tradThousand;

	        // remove extra spaces!
	        return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
	    }
	 
	 private static String convertLessThanOneThousand(int number) {
	        String soFar;

	        if (number % 100 < 20) {
	            soFar = numNames[number % 100];
	            number /= 100;
	        } else {
	            soFar = numNames[number % 10];
	            number /= 10;

	            soFar = tensNames[number % 10] + soFar;
	            number /= 10;
	        }
	        if (number == 0) {
	            return soFar;
	        }
	        return numNames[number] + " hundred" + soFar;
	    }
	 
	 
	 public StringBuilder getCandleSticktProperties(String ChartType) {
			StringBuilder candlestickProperties = new StringBuilder();

			candlestickProperties.append("<li class=\"general-filters active-filter\" data-column-name=\"" + ChartType
					+ "HOVERLABELDATA\" data-key-type=\"data\">" + "<div class=\"sub-filterItems\">"
					+ "<p>Hover Data Visible</p>" + "<select name=\"text-info\" id=\"" + ChartType
					+ "HOVERLABELDATA\" data-opt-name=\"hoverinfo\" >" + "<option value=\"x\">Label</option>"
					+ "<option value=\"y\">Value</option>" + "<option value=\"%\">Percentage</option>"
					+ "<option value=\"x+y\" selected>Label and value</option>"
					+ "<option value=\"x+%\">Label and Percentage</option>"
					+ "<option value=\"y+%\">Value and Percentage</option>" + "</select>" + "</div>" + "</li>");
			candlestickProperties.append(getChartHover(ChartType, "data"));
			
			return candlestickProperties;
		}
	 
	 
	 public RegistrationDTO setAllToRegistrationDTO(HttpServletRequest request, JSONObject basicData) {
	        RegistrationDTO registrationDTO = new RegistrationDTO();
	        String userFirstName = "";
	        try {
                String role = "ADMIN";
                basicData.put("add_role",role);
                
	            String userNameReq = (String) basicData.get("billing_name")+"_"+(String) basicData.get("billing_lastname")+"_MGR";
	            if(userNameReq !=  null && !"".equalsIgnoreCase(userNameReq) && !"null".equalsIgnoreCase(userNameReq)
	                    && userNameReq.contains("_")){
	                String[] detalisStringArr = userNameReq.split("_");
	                userFirstName = detalisStringArr[0];
	                userNameReq = userNameReq.toUpperCase();
	            }
	            String password = "P@ssw0rd";
	            basicData.put("confirm_password", password);
	            basicData.put("rsUsername",userNameReq);
	            registrationDTO.setAdditional_role((String) basicData.get("add_role") != null ? (String) basicData.get("add_role") : "");
	            registrationDTO.setAddress1((String) basicData.get("billing_city") != null ? (String) basicData.get("billing_city") : "");
	            registrationDTO.setAddress2((String) basicData.get("billing_state") != null ? (String) basicData.get("billing_state") : "");
	            registrationDTO.setConfirm_password((String) basicData.get("confirm_password") != null ? (String) basicData.get("confirm_password") : "");
	            registrationDTO.setCountry((String) basicData.get("billing_country") != null ? (String) basicData.get("billing_country") : "");
	            registrationDTO.setEmail_id((String) basicData.get("billing_email") != null ? (String) basicData.get("billing_email") : "");
	            registrationDTO.setExperience_summary((String) basicData.get("billing_company") != null ? (String) basicData.get("billing_company") : "");
	            registrationDTO.setFirst_name(userFirstName);

	            registrationDTO.setLast_name((String) basicData.get("billing_lastname") != null ? (String) basicData.get("billing_lastname") : "");
	            registrationDTO.setLocale((String) basicData.get("locale") != null ? (String) basicData.get("locale") : "en_US  ");
	            registrationDTO.setMobile_number((String) basicData.get("billing_tel") != null ? (String) basicData.get("billing_tel") : "");
	            registrationDTO.setMiddle_name((String) basicData.get("age") != null ? (String) basicData.get("age") : "");
	            registrationDTO.setMonth((String) basicData.get("month") != null ? (String) basicData.get("month") : "");
	            registrationDTO.setNick_name((String) basicData.get("jobtitle") != null ? (String) basicData.get("jobtitle") : "");
	            registrationDTO.setPassword(password);
	            registrationDTO.setDate((String) basicData.get("dob") != null ? (String) basicData.get("dob") : "01-01-1958");
	            registrationDTO.setDate_of_birth((String) basicData.get("dob") != null ? (String) basicData.get("dob") : "01-01-2000");

	            registrationDTO.setPhone_number((String) basicData.get("billing_zip") != null ? (String) basicData.get("billing_zip") : "");
	            registrationDTO.setPlant((String) basicData.get("plant") != null ? (String) basicData.get("plant") : "1000");
	            registrationDTO.setInstance((String) basicData.get("instance") != null ? (String) basicData.get("instance") : "100");
	            registrationDTO.setReport_to((String) basicData.get("report_to") != null ? (String) basicData.get("report_to") : "MM_MANAGER");
	            registrationDTO.setRole(role);
	            //registrationDTO.setUser_name((String) basicData.get("user_name") != null ? (String) basicData.get("user_name") : "");
	            registrationDTO.setUser_name((String) basicData.get("rsUsername") != null ? (String) basicData.get("rsUsername") : "");
	            registrationDTO.setYear((String) basicData.get("year") != null ? (String) basicData.get("year") : "");
	            registrationDTO.setFilepath(request.getParameter("filepath"));
	            registrationDTO.setOrgName((String) basicData.get("ORGN_NAME") != null ? (String) basicData.get("ORGN_NAME") : "SmartBI");
	            registrationDTO.setPurposeofReg((String) basicData.get("gender") != null ? (String) basicData.get("gender") : "MALE");
	            //usr_orgid
	        } catch (Exception e) {
	            logger.error(e.getLocalizedMessage());
	        }

	        return registrationDTO;
	    }
	 
	 
	 public JSONObject checkSubscriptedMailExists(HttpServletRequest request) {
	        return dashBoardsDAO.checkSubscriptedMailExists(request);
	    }
	 
	 public JSONObject checkForCompanyAlreadyExist(HttpServletRequest request) {
			return dashBoardsDAO.checkForCompanyAlreadyExist(request) ;
		 }


}
