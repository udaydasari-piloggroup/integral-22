/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pilog.mdm.Controller;

import com.pilog.mdm.Service.DashBoardsService;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Jagadish.K
 */
@Controller
public class DashBoardsController {          

	@Autowired
	public DashBoardsService dashBoardsService;

	@RequestMapping(value = { "/dataDXPAnalytics" }, method = { RequestMethod.POST, RequestMethod.GET }, produces = {
			"application/json" })
	public @ResponseBody JSONObject getDXPAnalyticstabsData(HttpServletRequest request) {     
		JSONObject resultObj = new JSONObject();
		JSONObject treeObj = new JSONObject();
		try {
			String treeId = request.getParameter("treeId");
			String result = dashBoardsService.getVisualizationLayout(request);
			JSONObject jsonChartFilter = dashBoardsService.getChartFilters(request);
			String buttonstr = dashBoardsService.getButtons();
			treeObj = dashBoardsService.getGenericDxpTreeOpt(request, treeId);
			resultObj.put("jsonChartFilterObj", jsonChartFilter);
			resultObj.put("result", result);
			resultObj.put("treeObj", treeObj);
			resultObj.put("Buttons", buttonstr);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = { "/getVisualizationDataSources" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getTreeDataMigrationConnections(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			String menuResult = "";
			String menuId = request.getParameter("menuId");
			JSONArray treeMenuArray = dashBoardsService.getDataMigrationConnectionsTreeMenu(request, menuId);
			if (treeMenuArray != null && !treeMenuArray.isEmpty()) {
				menuResult = treeMenuArray.toJSONString();
			}
			resultObj.put("menuResult", menuResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/importTreeChartsDMFileXlsx", produces = { "application/json" })
	public @ResponseBody JSONObject importDMFileXlsx(HttpServletRequest request, HttpServletResponse response) {

		System.out.println("Entered Export Controller...");
		JSONObject resultObj = new JSONObject();
		try {
			String selectedFiletype = request.getParameter("selectedFiletype");
			String jsonDataStr = request.getParameter("jsonData");
			JSONObject jsonData = (JSONObject) JSONValue.parse(jsonDataStr);
			resultObj = dashBoardsService.importTreeDMFileXlsx(request, response, jsonData, selectedFiletype);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getChartsFileObjectData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody List getFileObjectData(HttpServletRequest request, HttpServletResponse response) {

		List dataList = new ArrayList();
		try {
			dataList = dashBoardsService.getFileObjectData(request, response);
		} catch (Exception e) {
		}
		return dataList;
	}

	@RequestMapping(value = "/getLoadTableColumns", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getLoadTableColumns(HttpServletRequest request, HttpServletResponse response) {

		String result = "";
		try {
			result = dashBoardsService.getLoadTableColumns(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/fetchChartData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchChartData(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();  
		try {
			resultObj = dashBoardsService.fetchChartData(request);              
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchFiltersValues", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchFiltersValues(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchFiltersValues(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchSlicerValues", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchSlicerValues(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchSlicerValues(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchSlicerButtonValues", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchSlicerButtonValues(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchSlicerButtonValues(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchSlicerListValues", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchSlicerListValues(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchSlicerListValues(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchSlicerDropdownValues", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchSlicerDropdownValues(HttpServletRequest request,
			HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchSlicerDropdownValues(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/chartJoinTables", method = { RequestMethod.GET, RequestMethod.POST })
	public JSONObject chartJoinTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.chartJoinTables(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchChartJoinTablesData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchJoinTablesData(HttpServletRequest request) {

		JSONObject joinTablesData = new JSONObject();
		try {
			joinTablesData = dashBoardsService.fetchJoinTableColumnTrfnRules(request);

		} catch (Exception e) {
		}
		return joinTablesData;
	}

	@RequestMapping(value = "/fetchCardDetails", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchCardDetails(HttpServletRequest request) {

		JSONObject joinTablesData = new JSONObject();
		try {
			joinTablesData = dashBoardsService.fetchCardDetails(request);

		} catch (Exception e) {
		}
		return joinTablesData;
	}

	@RequestMapping(value = "/fetchHomeCardDetails", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchHomeCardDetails(HttpServletRequest request) {

		JSONObject joinTablesData = new JSONObject();
		try {
			joinTablesData = dashBoardsService.fetchHomeCardDetails(request);

		} catch (Exception e) {
		}
		return joinTablesData;
	}

	@RequestMapping(value = "/getchartElement", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getchartElement(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.getchartElement(request);

		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getchartchildElement", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String getchartchildElement(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.getchartchildElement(request);

		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/fetchpredictiveChartData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchpredictiveChartData(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchpredictiveChartData(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/saveVisualizeData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String saveVisualizeData(HttpServletRequest request, HttpServletResponse response) {

		String result = "";
		try {
			result = dashBoardsService.saveVisualizeData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	@RequestMapping(value = "/getChartData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getChartData(HttpServletRequest request, HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getChartData(request);
			JSONObject jsonChartFilter = dashBoardsService.getChartFilters(request);
			TablesDataobj.put("jsonChartFilterObj", jsonChartFilter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = "/getfiletColumnData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getfiletColumnData(HttpServletRequest request) {
		JSONObject jsonresult = new JSONObject();
		try {
			jsonresult = dashBoardsService.getfilterColumnData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonresult;

	}

	@RequestMapping(value = { "/getchartGrid" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = "application/json")
	public @ResponseBody JSONObject getchartGrid(HttpServletRequest request) {
		JSONObject gridObj = new JSONObject();
		try {
			gridObj = dashBoardsService.getchartGrid(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return gridObj;
	}

	@RequestMapping(value = "/ChartGridResults", method = RequestMethod.POST)
	public @ResponseBody List ChartGridResults(HttpServletRequest request) {
		List resultList = new ArrayList();
		try {
			String gridId = request.getParameter("gridId");
			String colsArray = request.getParameter("colsArray");
			String tableName = request.getParameter("tableName");
			if ((gridId != null && !"".equals(gridId)) && (colsArray != null && !"".equals(colsArray))
					&& (tableName != null && !"".equals(tableName))) {
				resultList = dashBoardsService.getSerachResults((List) JSONValue.parse(colsArray), tableName, gridId,
						request);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultList;
	}

	@RequestMapping(value = "/deleteVisualizeChart", method = { RequestMethod.POST,
			RequestMethod.GET }, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String deleteVisualizeChart(HttpServletRequest request) {
		String result = "";
		JSONObject resobj = new JSONObject();
		try {
			result = dashBoardsService.deleteVisualizeChart(request);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String create(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.create(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/updatechartdata", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String updatechartdata(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.updatechartdata(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/dashboardSetting", method = { RequestMethod.POST,
			RequestMethod.GET }, produces = "application/json")
	public @ResponseBody JSONObject dashboardSetting(HttpServletRequest request) {
		String result = "";
		JSONObject resultObj = new JSONObject();
		try {
			result = dashBoardsService.dashboardSetting(request);
			JSONObject jsonChartFilter = dashBoardsService.getChartFilters(request);
			resultObj.put("jsonChartFilterObj", jsonChartFilter);
			resultObj.put("filtercolumn", jsonChartFilter.get("filtercolumn"));
			resultObj.put("result", result);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/getconfigobject", method = { RequestMethod.POST,
			RequestMethod.GET }, produces = "application/json")
	public @ResponseBody JSONObject getconfigobject(HttpServletRequest request) {
		String result = "";
		JSONObject resultObj = new JSONObject();
		try {

			JSONObject getconfigobject = dashBoardsService.getconfigobject(request);
			resultObj.put("jsonChartFilterObj", getconfigobject);
			resultObj.put("result", result);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = { "/getCurrentDBTables" })
	public @ResponseBody JSONObject getCurrentDBTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {

			resultObj = dashBoardsService.getCurrentDBTables(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/getChartFilterData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getChartFilterData(HttpServletRequest request, HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getChartFilterData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = "/getHomeChartSlicerData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getHomeChartSlicerData(HttpServletRequest request, HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getHomeChartSlicerData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = "/fetchHomeSlicerValues", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchHomeSlicerValues(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchHomeSlicerValues(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/getSlicerHomeCharts", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getSlicerHomeCharts(HttpServletRequest request, HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getSlicerHomeCharts(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = { "/movingAvgData" })
	public @ResponseBody JSONObject movingAvgData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {

			resultObj = dashBoardsService.movingAvgData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/insertdata", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String insertdata(HttpServletRequest request) {
		String Result = "";
		try {
			Result = dashBoardsService.insertdata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;

	}

	@RequestMapping(value = "/getlandingGraphData", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody JSONObject getlandingGraphData(HttpServletRequest request) {
		JSONObject Result = new JSONObject();
		try {
			Result = dashBoardsService.getlandingGraphData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;

	}

	@RequestMapping(value = "/getdashbordname", method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody String getdashbordname(HttpServletRequest request) {
		String Result = "";
		try {
			Result = dashBoardsService.getdashbordname(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;

	}

	@RequestMapping(value = { "/jqxPivotGrid" }, method = { RequestMethod.POST, RequestMethod.GET })
	@ResponseBody
	public JSONObject getJqxPivotGrid(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			System.out.println("Enter into PivotGrid Controller");
			String gridId = request.getParameter("gridId");
			JSONObject gridResults = this.dashBoardsService.getJqxPivotGridData(gridId, request);
			resultObj.put("data", gridResults.get("data"));
			resultObj.put("datafields", gridResults.get("datafields"));
			resultObj.put("rows", gridResults.get("rows"));
			resultObj.put("columns", gridResults.get("columns"));
			resultObj.put("filters", gridResults.get("filters"));
			resultObj.put("values", gridResults.get("values"));
			resultObj.put("gridId", gridResults.get("gridId"));
			resultObj.put("resultString", gridResults.get("resultString"));
			resultObj.put("rowsResultString", gridResults.get("rowsResultString"));
			resultObj.put("columnsList", gridResults.get("columnsList"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = { "/pivotGrid" }, method = { RequestMethod.POST, RequestMethod.GET })
	@ResponseBody
	public JSONObject getPivotGrid(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			System.out.println("Enter into PivotGrid Controller");
			String gridId = request.getParameter("gridId");
			JSONObject gridResults = this.dashBoardsService.getPivotGridData(gridId, request);
			resultObj.put("data", gridResults.get("data"));
			resultObj.put("datafields", gridResults.get("datafields"));
			resultObj.put("rows", gridResults.get("rows"));
			resultObj.put("columns", gridResults.get("columns"));
			resultObj.put("filters", gridResults.get("filters"));
			resultObj.put("values", gridResults.get("values"));
			resultObj.put("gridId", gridResults.get("gridId"));
			resultObj.put("resultString", gridResults.get("resultString"));
			resultObj.put("rowsResultString", gridResults.get("rowsResultString"));
			resultObj.put("columnsList", gridResults.get("columnsList"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/updatechartSettingdata", method = RequestMethod.POST, produces = "text/plain;charset=UTF-8")
	public @ResponseBody String updatechartSettingdata(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.updatechartSettingdata(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getAnalyticsMetaData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getSchemaObjectMetaData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getSchemaObjectData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/getAnalyticsObjectData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONArray getSchemaObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		try {
			resultObj = dashBoardsService.getSchemaObjectData(request);
			dataArray = (JSONArray) resultObj.get("dataArray");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataArray;
	}

	@RequestMapping(value = "/gridUpdateRecords", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String gridUpdateRecords(HttpServletRequest request) {
		String result = "";
		JSONObject resultObj = new JSONObject();
		String gridDataString = request.getParameter("gridJsonData");
		String gridId = request.getParameter("gridId");
		String tableName = request.getParameter("tableName");
		try {

			if (gridDataString != null && !"".equalsIgnoreCase(gridDataString)) {
				JSONArray dataArray = (JSONArray) JSONValue.parse(gridDataString);
				for (int i = 0; i < dataArray.size(); i++) {
					JSONObject dataObj = (JSONObject) dataArray.get(i);
					result = dashBoardsService.gridUpdateRecords(request, dataObj, tableName, gridId);
					result = dataArray.size() + " Row(s) " + result;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/saveHomeChartsColorsData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject saveHomeChartsColorsData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.saveHomeChartsColorsData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/getSurveyHomeCharts", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getSurveyHomeCharts(HttpServletRequest request, HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getSurveyHomeCharts(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = "/updteCompareFilterColumn", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String updteCompareFilterColumn(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.updteCompareFilterColumn(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/updteCompareFilterColumnsData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String updteCompareFilterColumnsData(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.updteCompareFilterColumnsData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj.toJSONString();

	}

	@RequestMapping(value = "/getDrillDownFilterColumns", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getDrillDownFilterColumns(HttpServletRequest request) {
		JSONObject jsonresult = new JSONObject();
		try {
			jsonresult = dashBoardsService.getDrillDownFilterColumns(request);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonresult;

	}

	@RequestMapping(value = "/getHomeChartHeaderFilterForm", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getHomeChartHeaderFilterForm(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getHomeChartHeaderFilterForm(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = "/updteFilterColumn", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String updteFilterColumn(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.updteFilterColumn(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/createFilterHeader", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject createFilterHeader(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.createFilterHeader(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = { "/downloadChartImageAllPDF" }, method = RequestMethod.POST)
	public void downloadALLChartsInPDF(HttpServletRequest request, HttpServletResponse response, ModelMap model) {
		dashBoardsService.downloadALLChartsInPDF(request, response);
	}

	@RequestMapping(value = "/getChartHomePageDiv", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getChartHomePageDiv(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getChartHomePageDiv(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/showDrillDownChart", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject showDrillDownChart(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.showDrillDownChart(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/getcolorpalleteform", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getcolorpalleteform(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.getcolorpalleteform(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/getChartColumnsForm", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getChartColumnsForm(HttpServletRequest request, HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getChartColumnsForm(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = "/saveChartRadioButtonColumns", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String chartRadioButtonColumnForm(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.saveChartRadioButtonColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/getchartconfigobjdata", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getchartconfigobjdata(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.getchartconfigobjdata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/updateGraphProperties", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject updateGraphProperties(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.updateGraphProperties(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/getTreeMapExchangeLevels", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getTreeMapExchangeLevels(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.getTreeMapExchangeLevels(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/getExchaneLevelsData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getExchaneLevelsData(HttpServletRequest request, HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getExchaneLevelsData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = "/createTableasFile", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject createTableasFile(HttpServletRequest request, HttpServletResponse response) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		try {
			resultObj = dashBoardsService.createTableasFile(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/deleteTableColumn", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String deleteTableColumn(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();

		try {

			resultObj = dashBoardsService.deleteTableColumn(request);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj.toJSONString();
	}

	@RequestMapping(value = "/mergeformdata", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject mergeformdata(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.mergeformdata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/transformdata", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String transformdata(HttpServletRequest request) {
		String Result = "";
		try {
			Result = dashBoardsService.transformdata(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}

	@RequestMapping(value = "/gettransposedata", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String gettransposedata(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		String Result = "";
		try {
			Result = dashBoardsService.gettransposedata(request);
			dataArray = (JSONArray) resultObj.get("dataArray");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}

	@RequestMapping(value = "/DimensionTransposeColumn", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String DimensionTransposeColumn(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		String Result = "";
		try {
			Result = dashBoardsService.DimensionTransposeColumn(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}

	@RequestMapping(value = "/setImportData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody boolean setImportData(HttpServletRequest request) {
		boolean result = false;
		try {
			result = dashBoardsService.setImportData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/showtableData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject showtableData(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.showtableData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/gettableObjectData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONArray gettableObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		try {
			resultObj = dashBoardsService.gettableObjectData(request);
			dataArray = (JSONArray) resultObj.get("dataArray");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataArray;
	}

	@RequestMapping(value = "/gettableattribute", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String gettableattribute(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		String Result = "";
		try {
			Result = dashBoardsService.gettableattribute(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}

	@RequestMapping(value = "/caseSensitive", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String caseSensitive(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		String Result = "";
		try {
			Result = dashBoardsService.caseSensitive(request);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return Result;
	}

	@RequestMapping(value = "/DimensionTransposedata", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject DimensionTransposedata(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.DimensionTransposedata(request);
			dataArray = (JSONArray) resultObj.get("dataArray");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/generateQueryStr", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject generateQueryStr(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.generateQueryStr(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/getModalFileColumns", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getModalFileColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getModalFileColumns(request);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchModalChartData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchModalChartData(HttpServletRequest request, HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.fetchModalChartData(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/renameSQLColumn", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String renameSQLColumn(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.renameSQLColumn(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getAggregateResult", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getAggregateResult(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.getAggregateResult(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/createSuffixAndPriffix", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String createSuffixAndPriffix(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.createSuffixAndPriffix(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/updatePalatteColor", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody String updatePalatteColor(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.updatePalatteColor(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/getDataCorrelation", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getDataCorrelaltion(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getDataCorrelation(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = { "/getAutoSuggestedChartTypes" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getAutoSuggestedChartTypes(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getAutoSuggestedChartTypes(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = { "/getDateColumns" }, method = { RequestMethod.POST, RequestMethod.GET }, produces = {
			"application/json" })
	public @ResponseBody JSONObject getDateColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getDateColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = { "/getQueryGridData" }, method = { RequestMethod.POST, RequestMethod.GET }, produces = {
			"application/json" })
	public @ResponseBody JSONObject getQueryGridData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getQueryGridData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/getChartObjectData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONArray getChartObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		try {
			resultObj = dashBoardsService.getChartObjectData(request);
			dataArray = (JSONArray) resultObj.get("dataArray");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataArray;
	}

	@RequestMapping(value = "/viewAnalyticsTableGrid", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject viewAnalyticsTableDataGrid(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.viewAnalyticsTableDataGrid(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/viewAnalyticsTableGridData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONArray viewAnalyticsTableGridData(HttpServletRequest request) {
		JSONArray dataArray = new JSONArray();
		try {
			JSONObject resultObj = dashBoardsService.viewAnalyticsTableDataGrid(request);
			dataArray = (JSONArray) resultObj.get("data");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataArray;
	}

	@RequestMapping(value = "/executeBISQLQuery", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject executeSQLQuery(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.executeSQLQuery(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = { "/getSuggestedChartTypesBasedonColumns" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getSuggestedChartTypesBasedonColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getSuggestedChartTypesBasedonColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = { "/getCurrencyAndCode" }, method = { RequestMethod.POST, RequestMethod.GET })
	public @ResponseBody String getCurrencyAndCodeList(HttpServletRequest request) {
		String currencyAndCodeHtmlData = null;
		try {
			currencyAndCodeHtmlData = dashBoardsService.getCurrencyAndCodesData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return currencyAndCodeHtmlData;
	}

	@RequestMapping(value = "/getAutoSuggestedFilterTables", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getAutoSuggestedFilterTables(HttpServletRequest request,
			HttpServletResponse response) {
		JSONObject TablesDataobj = new JSONObject();
		try {
			TablesDataobj = dashBoardsService.getAutoSuggestedFilterTables(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return TablesDataobj;

	}

	@RequestMapping(value = { "/getArtificialIntellisenseApiDetails" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getArtificialIntellisenseApiDetails(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getArtificialIntellisenseApiDetails(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/alterBiTableCol", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject alterBiTable(HttpServletRequest request) {

		JSONObject resultObj = new JSONObject();
		try {
			System.out.println("alterBiTable controller calling......");
			resultObj = dashBoardsService.alterBiTable(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/getColumnformStr", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getColumnformStr(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.gettableformStr(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getSelectType", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getSelectType(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.getSelectType(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getSuffixValue", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getSuffixValue(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.getSuffixValue(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getPrefixValue", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getPrefixValue(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.getPrefixValue(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getCreateFind", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getCreateFind(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.getCreateFind(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/getRenameValue", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String getRenameValue(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.getRenameValue(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/executeAlterTableColumn", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject executeAlterTableColumn(HttpServletRequest request) {

		JSONObject resultObj = new JSONObject();
		try {

			resultObj = dashBoardsService.executeAlterTable(request); // executeAlterTable
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/createPrefixValue", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String createPrefixValue(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.createPrefixValue(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/deleterowdata", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String deleterowdata(HttpServletRequest request, HttpServletResponse response) {
		String result = "";
		try {
			result = dashBoardsService.deleterowdata(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/removeDuplicateValue", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject removeDuplicateValue(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.removeDuplicateValue(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/removeDuplicateEachColumn", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject removeDuplicateEachColumn(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.removeDuplicateEachColumn(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/deleteDuplicateValues", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject deleteDuplicateValues(HttpServletRequest request) {
		JSONObject resultobj = new JSONObject();
		try {
			resultobj = dashBoardsService.deleteDuplicateValues(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultobj;
	}

	@RequestMapping(value = "/executeBIPythonQuery", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject executePythonQuery(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.executePythonQuery(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/getPythonChartObjectData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONArray getPythonChartObjectData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		try {
			resultObj = dashBoardsService.getPythonChartObjectData(request);
			dataArray = (JSONArray) resultObj.get("dataArray");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataArray;
	}

	@RequestMapping(value = { "/getCardDateValues" }, method = { RequestMethod.POST, RequestMethod.GET }, produces = {
			"application/json" })
	public @ResponseBody JSONObject getCardDateValues(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getCardDateValues(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	
	
	
	@RequestMapping(value = "/saveImageOnServer", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject saveImageOnServer(HttpServletRequest request,
			@RequestParam("fileToBeUploaded") MultipartFile fileData) {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject = dashBoardsService.saveImageOnServer(request, fileData); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}

	@RequestMapping(value = "/getCardImageData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONObject getCardImageData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getCardImageData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}

	@RequestMapping(value = "/getCardImgData", method = { RequestMethod.GET,
			RequestMethod.POST }, produces = "application/json")
	public @ResponseBody JSONArray getCardImgData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		JSONArray dataArray = new JSONArray();
		try {
			resultObj = dashBoardsService.getCardImgData(request);
			dataArray = (JSONArray) resultObj.get("dataArray");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataArray;
	}

	@RequestMapping(value = "/chartDxpJoinTables", method = { RequestMethod.GET, RequestMethod.POST })
	public JSONObject chartDxpJoinTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.chartDxpJoinTables(request);
		} catch (Exception e) {
		}
		return resultObj;
	}

	@RequestMapping(value = "/fetchChartJoinDxpTablesData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject fetchChartJoinDxpTablesData(HttpServletRequest request) {

		JSONObject joinTablesData = new JSONObject();
		try {
			joinTablesData = dashBoardsService.fetchChartJoinDxpTablesData(request);
//            joinTablesData = dataPipingService.fetchJoinTablesData(request);
		} catch (Exception e) {
		}
		return joinTablesData;
	}

	@RequestMapping(value = "/importTreeDMFinanceFile", produces = "text/plain;charset=UTF-8")
	public @ResponseBody String importDMFile(HttpServletRequest request, HttpServletResponse response,
			@RequestParam("selectedFiletype") String selectedFiletype,
			@RequestParam("fileLocalPath") String fileLocalPath, @RequestParam("importTreeDMFile") MultipartFile file) {

		System.out.println("Entered Export Controller...");
		String result = "";
		try {

			result = dashBoardsService.importTreeDMFile(request, response, file, selectedFiletype);

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("result::::" + result);
		return result;
	}

	@RequestMapping(value = "/getChatBotResponse", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getChatBotResponse(HttpServletRequest request) {
		JSONObject resultObject = null;
		try {
			resultObject = dashBoardsService.getChatBotResponse(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}

	@RequestMapping(value = "/getUserTableNames", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getUserTableNames(HttpServletRequest request) {
		JSONObject resultObject = null;
		try {
			resultObject = dashBoardsService.getUserTableNames(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}

	@RequestMapping(value = "/importIntelliSenseTreeChartsDMFileXlsx", produces = { "application/json" })
	public @ResponseBody JSONObject importIntelliSenseTreeChartsDMFileXlsx(HttpServletRequest request,
			HttpServletResponse response) {

		System.out.println("Entered Export Controller...");
		JSONObject resultObj = new JSONObject();
		try {
			String selectedFiletype = request.getParameter("selectedFiletype");
			String jsonDataStr = request.getParameter("jsonData");
			JSONObject jsonData = (JSONObject) JSONValue.parse(jsonDataStr);
			resultObj = dashBoardsService.importIntelliSenseTreeDMFileXlsx(request, response, jsonData,
					selectedFiletype);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/createIntelliSenseTableasFile", produces = { "application/json" })
	public @ResponseBody JSONObject createIntelliSenseTableasFile(HttpServletRequest request,
			HttpServletResponse response) {

		JSONObject resultObj = new JSONObject();
		try {
			String mainFileName = request.getParameter("filePath");
			resultObj = dashBoardsService.createIntelliSenseTableasFile(request, response, mainFileName);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseTableColumns", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseTableColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseTableColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseChartTypes", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseChartTypes(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseChartTypes(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseChartColumns", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseChartColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseChartColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseChartConfig", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseChartConfig(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseChartConfig(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseExampleChartDesign", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseExampleChartDesign(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseExampleChartDesign(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseChartSubColumns", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseChartSubColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseChartSubColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseViewFilters", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseViewFilters(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseViewFilters(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseViewFiltersValues", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseViewFiltersValues(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getIntelliSenseViewFiltersValues(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getIntelliSenseViewModalChartConfigOptions", produces = { "application/json" })
	public @ResponseBody JSONObject getIntelliSenseViewModalChartConfigOptions(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			JSONObject jsonChartFilter = dashBoardsService.getChartFilters(request);
			resultObj.put("jsonChartFilterObj", jsonChartFilter);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getEditorMergeTableNames", produces = { "application/json" })
	public @ResponseBody JSONObject getEditorMergeTableNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getEditorMergeTableNames(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getEditorMergeTableColumns", produces = { "application/json" })
	public @ResponseBody JSONObject getEditorMergeTableColumns(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getEditorMergeTableColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/checkExistMergeTableName", produces = { "application/json" })
	public @ResponseBody JSONObject checkExistMergeTableName(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.checkExistMergeTableName(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/createTableANdJoinTables", produces = { "application/json" })
	public @ResponseBody JSONObject createTableANdJoinTables(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.createTableANdJoinTables(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/insertMergeTablesData", produces = { "application/json" })
	public @ResponseBody JSONObject insertMergeTablesData(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.insertMergeTablesData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultObj;
	}

	@RequestMapping(value = "/getChatRplyResponse", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getChatRplyResponse(HttpServletRequest request) {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject = dashBoardsService.getChatRplyResponse(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}

	@RequestMapping(value = "/getConvAIMergeTableColumns", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getConvAIMergeTableColumns(HttpServletRequest request) {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject = dashBoardsService.getConvAIMergeTableColumns(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}

	@RequestMapping(value = "/updateCardData", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String updateCardData(HttpServletRequest request) {

		String result = "";
		try {
			result = dashBoardsService.updateCardData(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	@RequestMapping(value = "/deleteDashboard", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody String deleteDashboard(HttpServletRequest request) {
		String result = "";
		try {
			result = dashBoardsService.deleteDashboard(request);
		} catch (Exception e) {
		}
		return result;
	}

	@RequestMapping(value = "/saveFileOnServer", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject saveFileOnServer(HttpServletRequest request,
			@RequestParam("fileToBeUploaded") MultipartFile fileData) {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject = dashBoardsService.saveFileOnServer(request, fileData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}
	
	
	@RequestMapping(value = { "/getEditDashBoardNames" }, method = { RequestMethod.POST, RequestMethod.GET }, produces = {
			"application/json" })
	public @ResponseBody JSONObject getEditDashBoardNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getEditDashBoardNames(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	@RequestMapping(value = { "/getSaveDashBoardNames" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getSaveDashBoardNames(HttpServletRequest request) {
		JSONObject resultObj = new JSONObject();
		try {
			resultObj = dashBoardsService.getSaveDashBoardNames(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObj;
	}
	
	@RequestMapping(value = { "/getWeatherDetailsFromCity" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getWeatherDetailsFromCity(HttpServletRequest request) {

		JSONObject response = new JSONObject();       
		try {
			response = dashBoardsService.getWeatherDetailsByCity(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	
	@RequestMapping(value = { "/getChartNotes" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject getChartNotes(HttpServletRequest request) {

		JSONObject response = new JSONObject();       
		try {
			response = dashBoardsService.getChartNotes(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	
	@RequestMapping(value = { "/saveChartNotes" }, method = { RequestMethod.POST,
			RequestMethod.GET }, produces = { "application/json" })
	public @ResponseBody JSONObject saveChartNotes(HttpServletRequest request) {

		JSONObject response = new JSONObject();       
		try {
			response = dashBoardsService.saveChartNotes(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
	
	
	@RequestMapping(value = "/getVoiceResponse", method = { RequestMethod.GET, RequestMethod.POST })
	public @ResponseBody JSONObject getVoiceResponse(HttpServletRequest request) {
		JSONObject resultObject = new JSONObject();
		try {
			resultObject = dashBoardsService.getVoiceResponse(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultObject;
	}
	
	
	@RequestMapping(value={"/getIGFeaturesInfo"}, method = {RequestMethod.POST, RequestMethod.GET}, produces={"application/json"})
	  @ResponseBody
	  public JSONObject getLGFeaturesInfo(HttpServletRequest req, HttpServletResponse response)
	  {
	    JSONObject result = new JSONObject();
	    try
	    {
	      result = dashBoardsService.getLGFeaturesInfo(req);
	      System.out.println("datalist::::::::::::::::::" + result);
	    }
	    catch (Exception localException) {}
	    return result;
	  }
	 
	 //@CrossOrigin(origins = "https://integraldataanalytics.com", allowedHeaders = "Requestor-Type", exposedHeaders = "X-Get-Header")
	 @RequestMapping(value = "/getIGinfo", method = {RequestMethod.GET, RequestMethod.POST})
	    public String getinfo(ModelMap model, HttpServletRequest request, HttpServletResponse response) {
	        StringBuilder result = new StringBuilder();
	        try {
	            result = dashBoardsService.getinfo(request);
	            model.addAttribute("result", result);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return "leangovernance";
	    }
	 
	 @RequestMapping(value = "/getstate", method = {RequestMethod.GET, RequestMethod.POST})
	    public @ResponseBody
	    String getstate(HttpServletRequest request) {
	        JSONObject resultobj = new JSONObject();
	        List statelist = new ArrayList();
	        String result = "";
	        try {
	            result = dashBoardsService.getstate(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return result;
	    }
	 
	 @RequestMapping(value = "/getCity", method = {RequestMethod.POST, RequestMethod.GET})
	    public @ResponseBody
	    JSONObject getCity(HttpServletRequest request, HttpServletResponse response){
	        JSONObject resultObj = new JSONObject();
	        try {
	            resultObj=dashBoardsService.getCity(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	 
	 @RequestMapping(value = "/addOnpackage", method = {RequestMethod.GET, RequestMethod.POST})
	    public @ResponseBody
	    JSONObject addOnpackage(HttpServletRequest request) {
	        JSONObject resulobj = new JSONObject();
	        try {
	            resulobj = dashBoardsService.addOnpackage(request);
	        } catch (Exception e) {

	        }
	        return resulobj;
	    }
	 
	 @RequestMapping(value = "/getverificationcode", method = {RequestMethod.GET, RequestMethod.POST})
	    public @ResponseBody
	    JSONObject getverificationcode(HttpServletRequest request, HttpServletResponse response) {
	        JSONObject resultobj = new JSONObject();
	        try {
	            System.out.println("com.pilog.mdm.cloud.ws.Controller.DashBoardsController.getverificationcode()");
	            resultobj = dashBoardsService.getverificationcode(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultobj;
	    }
	 
	 @RequestMapping(value = "/getOTPVerificationcode", method = {RequestMethod.GET, RequestMethod.POST})
	    public @ResponseBody
	    JSONObject getOTPVerificationcode(HttpServletRequest request, HttpServletResponse response) {
	        JSONObject resultobj = new JSONObject();
	        try {
	            resultobj = dashBoardsService.getOTPVerificationcode(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultobj;
	    }
	 
	 @RequestMapping(value = "/getApplyDiscountCode", method = {RequestMethod.POST, RequestMethod.GET})
	    public @ResponseBody
	    JSONObject getApplyDiscountCode(HttpServletRequest request, HttpServletResponse response){
	        JSONObject resultObj = new JSONObject();
	        try {
	            resultObj=dashBoardsService.getApplyDiscountCode(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    } 
	 
	 @RequestMapping(value = "/paymentRequest", method = {RequestMethod.POST, RequestMethod.GET}, produces = "text/plain;charset=UTF-8")
	    public @ResponseBody
	    String ccAvenuePaymentRequest(
	            HttpServletRequest request, ModelMap map) {
	        return dashBoardsService.getCcAvenueResponsePageUrl(request);
	    }
	 
	 @RequestMapping(value = "/setIGInfo", method = {RequestMethod.POST, RequestMethod.GET})
	    public String setInfo(ModelMap model, HttpServletRequest request, HttpServletResponse response) {
	        String result = "";
	        try {
	            result = dashBoardsService.setInfo(request);
	            model.addAttribute("result", result);
	            model.addAttribute("class", "subscriptionInfoClass");
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return "leangovernance";
	    }
	 
	 @RequestMapping(value = "/checkMailExists", method = {RequestMethod.POST, RequestMethod.GET})
	    public @ResponseBody
	    JSONObject checkSubscriptedMailExists(HttpServletRequest request, HttpServletResponse response){
	        JSONObject resultObj = new JSONObject();
	        try {
	            resultObj=dashBoardsService.checkSubscriptedMailExists(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }
	 
	 @RequestMapping(value = "/checkForCompanyAlreadyExist", method = {RequestMethod.POST, RequestMethod.GET})
	    public @ResponseBody
	    JSONObject checkForCompanyAlreadyExist(HttpServletRequest request){
	        JSONObject resultObj = new JSONObject();
	        try {
	            resultObj=dashBoardsService.checkForCompanyAlreadyExist(request);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return resultObj;
	    }


}
