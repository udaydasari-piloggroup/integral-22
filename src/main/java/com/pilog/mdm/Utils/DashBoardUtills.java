/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pilog.mdm.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.opencsv.CSVReader;
import com.pilog.mdm.access.DataAccess;
import com.pilog.mdm.utilities.PilogUtilities;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 *
 * @author Jagadish.K
 */
@Repository
public class DashBoardUtills {
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
	@Value("${file.store.homedirectory}")
	private String fileStoreHomedirectory;
	private PilogUtilities cloudUtills = new PilogUtilities();

	public String getOracleDataTypeOfValue(String value, int length) {
		String dataType = "";
		try {
			if (value != null) {
				if (isNumeric(value)) {
					dataType = "NUMBER";
				} else if (isValidDate(value)) {
					dataType = "DATE";
				} else if (isBooleanValue(value)) {
					dataType = "VARCHAR2(10)";
				} else if (isCharacter(value)) {
					dataType = "VARCHAR2(4)";
				} else {
//                    dataType = "VARCHAR2(" + (length + 100) + ")";
					dataType = "VARCHAR2(4000)";
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

	public Connection getCurrentConnection() {
		Connection connection = null;
		try {
			Class.forName(dataBaseDriver);
			connection = DriverManager.getConnection(dbURL, userName, password);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return connection;
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

	public List fileHeaderValidations(List<String> headers) {
		try {
			String[] reservedKeyWords = { "ACCESS", "ELSE", "MODIFY", "START", "ADD", "EXCLUSIVE", "NOAUDIT", "SELECT",
					"ALL", "EXISTS", "NOCOMPRESS", "SESSION", "ALTER", "FILE", "NOT", "SET", "AND", "FLOAT", "NOTFOUND",
					"SHARE", "ANY", "FOR", "NOWAIT", "SIZE", "ARRAYLEN", "FROM", "NULL", "SMALLINT", "AS", "GRANT",
					"NUMBER", "SQLBUF", "ASC", "GROUP", "OF", "SUCCESSFUL", "AUDIT", "HAVING", "OFFLINE", "SYNONYM",
					"BETWEEN", "IDENTIFIED", "ON", "SYSDATE", "BY", "IMMEDIATE", "ONLINE", "TABLE", "CHAR", "IN",
					"OPTION", "THEN", "CHECK", "INCREMENT", "OR", "TO", "CLUSTER", "INDEX", "ORDER", "TRIGGER",
					"COLUMN", "INITIAL", "PCTFREE", "UID", "COMMENT", "INSERT", "PRIOR", "UNION", "COMPRESS", "INTEGER",
					"PRIVILEGES", "UNIQUE", "CONNECT", "INTERSECT", "PUBLIC", "UPDATE", "CREATE", "INTO", "RAW", "USER",
					"CURRENT", "IS", "RENAME", "VALIDATE", "DATE", "LEVEL", "RESOURCE", "VALUES", "DECIMAL", "LIKE",
					"REVOKE", "VARCHAR", "DEFAULT", "LOCK", "ROW", "VARCHAR2", "DELETE", "LONG", "ROWID", "VIEW",
					"DESC", "MAXEXTENTS", "ROWLABEL", "WHENEVER", "DISTINCT", "MINUS", "ROWNUM", "WHERE", "DROP",
					"MODE", "ROWS", "WITH" };
			List reservedKeyWordsList = Arrays.asList(reservedKeyWords);
			headers = headers.stream().map(col -> ((String) col).replaceAll("[^a-zA-Z0-9]", "_"))
					.collect(Collectors.toList());

			List tempHeadersList = new ArrayList();
			Map duplicateHeadersList = new HashMap();

			for (int i = 0; i < headers.size(); i++) {
				String col = headers.get(i);
				if (tempHeadersList.contains(col)) {
					duplicateHeadersList.put(col,
							(duplicateHeadersList.get(col) != null) ? ((int) duplicateHeadersList.get(col) + 1) : 1);
					col = col + duplicateHeadersList.get(col);
				}
				if (reservedKeyWordsList.contains(col.toUpperCase())) {
					duplicateHeadersList.put(col,
							(duplicateHeadersList.get(col) != null) ? ((int) duplicateHeadersList.get(col) + 1) : 1);
					col = col + duplicateHeadersList.get(col);
				}
				tempHeadersList.add(col);
			}
			headers = tempHeadersList;
			headers = headers.stream().map(col -> {

				if (((String) col).length() > 32) {
					col = (String) col;
					col = col.substring(col.length() - 31);
				}
				return col;
			}).collect(Collectors.toList());

			headers = headers.stream().map(col -> {
				if (Character.isDigit(((String) col).charAt(0))) {
					col = (String) col;
					col = col.replace(String.valueOf(col.charAt(0)), "A" + col.charAt(0));
				}
				if (col.startsWith("_")) {
					col = (String) col;
					col = col.replace(String.valueOf(col.charAt(0)), "");
				}
				return col;
			}).collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return headers;
	}

	public List readExcelFile(HttpServletRequest request, String filepath, String fileName) {

		FileInputStream fis = null;
		List dataList = new ArrayList();
		int rowVal = 1;
		try {
			Workbook workBook = WorkbookFactory.create(new File(filepath));
			Sheet sheet = null;
			int noOfSheets = workBook.getNumberOfSheets();

			String fileExtension = filepath.substring(filepath.lastIndexOf(".") + 1, filepath.length());

			if (workBook.getSheetAt(0) instanceof XSSFSheet) {
				sheet = (XSSFSheet) workBook.getSheetAt(0);
			} else if (workBook.getSheetAt(0) instanceof HSSFSheet) {
				sheet = (HSSFSheet) workBook.getSheetAt(0);
			}
			int lastRowNo = sheet.getLastRowNum();
			int firstRowNo = sheet.getFirstRowNum();
//                System.out.println("firstRowNo::::" + firstRowNo);
			int rowCount = lastRowNo - firstRowNo;
//                System.out.println("rowCount:::::" + rowCount);

			for (int i = rowVal; i <= lastRowNo; i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
//                        JSONObject dataObject = new JSONObject();
					Object[] dataObject = new Object[row.getLastCellNum()];
					// JSONObject dataObject = new JSONObject();
					// dataObject.put("totalrecords", rowCount);
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
//                                                dataObject.put(fileName + ":" + columnList.get(cellIndex), cellValue);
											dataObject[cellIndex] = cellValue;
										} else {
											dataObject[cellIndex] = "";
										}

										break;
									case Cell.CELL_TYPE_BOOLEAN:
//                                rowObj.put(header, hSSFCell.getBooleanCellValue());
										break;
									case Cell.CELL_TYPE_NUMERIC:

										if (HSSFDateUtil.isCellDateFormatted(cell)) {
											CellStyle cellStyle = cell.getCellStyle();
											Date cellDate = cell.getDateCellValue();
											String cellDateString = "";
											if ((cellDate.getYear() + 1900) == 1899 && (cellDate.getMonth() + 1) == 12
													&& (cellDate.getDate()) == 31) {
												cellDateString = (cellDate.getHours()) + ":" + (cellDate.getMinutes())
														+ ":" + (cellDate.getSeconds());
//                                                    System.out.println("cellDateString :: "+cellDateString);
											} else {
												cellDateString = (cellDate.getYear() + 1900) + "-"
														+ (cellDate.getMonth() + 1) + "-" + (cellDate.getDate());
											}

//                                                dataObject.put(fileName + ":" + columnList.get(cellIndex), cellDateString);
											dataObject[cellIndex] = cellDateString;

										} else {
											String cellvalStr = NumberToTextConverter
													.toText(cell.getNumericCellValue());
											dataObject[cellIndex] = cellvalStr;
										}
										break;
									case Cell.CELL_TYPE_BLANK:
										dataObject[cellIndex] = "";
										break;
								}

							} else {
								dataObject[cellIndex] = "";
							}
						} catch (Exception e) {
							dataObject[cellIndex] = "";
							continue;
						}

					} // end of row cell loop
					dataList.add(dataObject);
				}

			} // row end

			// return result1;
			if (fis != null) {
				fis.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

		return dataList;
	}

	public List getHeaderDataTypesOfImportedFile(HttpServletRequest request, String filePath) {
		List<String> headerTypeList = new ArrayList();
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
													headerTypeList.add("VARCHAR2(4000)");
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
													headerType = getBlankCellHeaderTypeRange(sheet, 2, j, headerType);
													headerTypeList.add(headerType);
													break;
											}

										} else {
											String headerType = "";
											headerType = getBlankCellHeaderTypeRange(sheet, 2, j, headerType);
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

					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return headerTypeList;
	}

	public List getHeadersOfImportedFile(HttpServletRequest request, HttpServletResponse response, String filePath) {
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

	public List<String> getCurrencyConvertedData(HttpServletRequest request, String fromCurrencyCode,
			String toCurrencyCode) {
		List<String> conversionRate = null;
		try {
			String query = "SELECT CONVERTION_RATE FROM B_CURRENCY_CONVERTION "
					+ "WHERE FROM_CURRENCY = :fromCurrencyCode AND TO_CURRENCY = :toCurrencyCode";
			Map<String, Object> treeMap = new HashMap<>();
			treeMap.put("fromCurrencyCode", fromCurrencyCode);
			treeMap.put("toCurrencyCode", toCurrencyCode);
			System.out.println("query::" + query);
			System.out.println("treeMap:::" + treeMap);
			conversionRate = access.sqlqueryWithParams(query, treeMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conversionRate;
	}

	@SuppressWarnings("unchecked")
	public <T> T getRequiredObjectTypeFromObject(Object object, String fromType, String toType) {
		if (fromType != null && !"".equalsIgnoreCase(fromType) && toType != null && !"".equalsIgnoreCase(toType)) {
			if ("BigDecimal".equalsIgnoreCase(fromType) && "Double".equalsIgnoreCase(toType)) {
				BigDecimal chartValueInBigDecimal = (BigDecimal) object;
				double chartValueIndouble = chartValueInBigDecimal.doubleValue();
				return (T) new Double(chartValueIndouble);
			}
			// TODO write for other data types if needed
		}
		return null;
	}

	public String convertTextToTitleCase(String label) {
		String titleCaseLabel = "";
		if (label != null && !"".equalsIgnoreCase(label)) {
			titleCaseLabel = Stream.of(label.trim().split("\\s")).filter(word -> word.length() > 0)
					.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
					.collect(Collectors.joining(" "));
		}
		return titleCaseLabel;
	}

	public boolean saveFileOnServer(String fileUploadDirectory, String fileName, MultipartFile multipartFile)
			throws IOException {
		boolean isFileUploaded = false;
		Path uploadPath = Paths.get(fileUploadDirectory);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}
		try (InputStream inputStream = multipartFile.getInputStream()) {
			Path filePath = uploadPath.resolve(fileName);
			Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
			isFileUploaded = true;
		} catch (IOException ioe) {
			isFileUploaded = false;
			throw new IOException("Could not save image file: " + fileName, ioe);
		}
		return isFileUploaded;
	}

	public String getImageBase64EncodedString(String fileUploadDirectory, String fileName) {
		String encodedString = "";
		try {
			String filePath = fileUploadDirectory + "/" + fileName;
			byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath));
			encodedString = Base64.getEncoder().encodeToString(fileContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encodedString;
	}

	public boolean isNullOrEmpty(String content) {
		return content == null || ("").equalsIgnoreCase(content) || ("null").equalsIgnoreCase(content);
	}

	public String getBlankCellHeaderTypeRange(Sheet sheet, int rowNum, int cellNum, String headerType) {
		Row row = sheet.getRow(rowNum);
		if (row != null) {
			Cell cell = row.getCell(cellNum);
			if (cell != null) {
				switch (cell.getCellType()) {
					case Cell.CELL_TYPE_STRING:
						String dataFormatString = cell.getCellStyle().getDataFormatString();
						headerType = "VARCHAR2(4000)";
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
						headerType = getBlankCellHeaderTypeRange(sheet, rowNum, cellNum, headerType);

						break;
				}
			} else {
				rowNum++;
				headerType = getBlankCellHeaderTypeRange(sheet, rowNum, cellNum, headerType);
			}
		}

		return headerType;
	}

	@SuppressWarnings("unused")
	public List readCSV(HttpServletRequest request, HttpServletResponse response, String filepath,
			List<String> columnList) {
		FileInputStream fis = null;
		List dataList = new ArrayList();
		int rowVal = 1;
		try {

			int rowCount = 0;
			// fis = new FileInputStream(new File(filepath));
			// char columnSeparator = '\t';
			// char columnSeparator = ',';
			CsvParserSettings settings = new CsvParserSettings();
			settings.detectFormatAutomatically();

			CsvParser parser = new CsvParser(settings);
			List<String[]> rows = parser.parseAll(new File(filepath));

			// if you want to see what it detected
			CsvFormat format = parser.getDetectedFormat();
			char columnSeparator = format.getDelimiter();
			String fileExtension = filepath.substring(filepath.lastIndexOf(".") + 1, filepath.length());
			if (".json".equalsIgnoreCase(fileExtension)) {
				columnSeparator = ',';
			}
			CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filepath), "UTF8"),
					columnSeparator);
			LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(filepath));
//	            System.out.println("fileExtension:::" + fileExtension);
			int stmt = 1;
			String strToDateCol = "";
			// need to write logic for extraction from File
//	            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filepath), "UTF8"), columnSeparator);
//	            LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(filepath));
			lineNumberReader.skip(Long.MAX_VALUE);
			long totalRecords = lineNumberReader.getLineNumber();
			if (totalRecords != 0) {
				totalRecords = totalRecords - 1;
			}

			rowVal = 1;

			int skipLines = 0;

			if (skipLines == 0) {
				String[] headers = reader.readNext();
				if (headers.length != 0 && headers[0].contains("" + columnSeparator)) {
					headers = headers[0].split("" + columnSeparator);
				}
			}
			reader.skip(skipLines);

			String[] nextLine;
			int rowsCount = 1;
			while ((nextLine = reader.readNext()) != null) {// no of rows
				if (nextLine.length != 0 && nextLine[0].contains("" + columnSeparator)) {
					nextLine = nextLine[0].split("" + columnSeparator);
				}

//	                    JSONObject dataObject = new JSONObject();
				Object[] dataObject = new Object[columnList.size()];
				// dataObject.put("totalrecords", totalRecords);
				for (int j = 0; j < columnList.size(); j++) {
					try {
						int cellIndex = j;
						if (cellIndex <= (nextLine.length - 1)) {
							String token = nextLine[cellIndex];
							if (token != null && !"".equalsIgnoreCase(token)) {
								try {
//	                                        dataObject.put(fileName + ":" + columnList.get(j), token);
									dataObject[j] = token;
								} catch (Exception e) {
									dataObject[j] = "";
									continue;
								}
							} else {
								dataObject[j] = "";
							}
						} else {
							dataObject[j] = "";
						}
					} catch (Exception e) {
						dataObject[j] = "";
						continue;
					}

				}

				dataList.add(dataObject);

			}

			if (fis != null) {
				fis.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dataList;
	}
	
	
//for TreeMap dynamic levels
public JSONObject getChildObj(JSONObject objData1, String match) {
		// TODO Auto-generated method stub
		int count = getCount(objData1);
		JSONArray childrenArray = new JSONArray();
		JSONObject childObject = new JSONObject();
		for(int j=0;j<count&&objData1.containsKey("children");j++) {
			if(objData1.get("name").equals(match)) {
				childObject = objData1;
				break;
			}
			
			childrenArray = (JSONArray) objData1.get("children");
			objData1 = (JSONObject) childrenArray.get(0);
		}
		
        // Search for the object with the specified name dynamically
		
//        for (int i = 0; i < childrenArray.size(); i++) {
//            childObject = (JSONObject) childrenArray.get(i);
//           
//        }
		return objData1;
	}

	private int getCount(JSONObject objData1) {
		// TODO Auto-generated method stub
		String jsonString = objData1.toString();	
		Pattern pattern = Pattern.compile("children");
        java.util.regex.Matcher matcher = pattern.matcher(jsonString);

        int count = 0;
        while (matcher.find()) {
            count++;
        }
		return count;
	}

	public JSONArray insertChildObj(JSONArray dataArr,JSONObject childObj,int x,String xAxisVal,String parentName,JSONArray childArr) {
		// TODO Auto-generated method stub
		JSONArray subChildArr = new JSONArray();	
		subChildArr.add(childObj);
		for (int i = 0; i < dataArr.size(); i++) {
			JSONObject childObject =(JSONObject) dataArr.get(i);
			if (childObject.get("name").toString().equals(parentName)) {
				JSONArray childrenArray = (JSONArray) childObject.get("children");
				for (int z = 0; z < childrenArray.size(); z++) {
					JSONObject objData1 = (JSONObject) childrenArray.get(z);
					if(x>=2) {
						objData1 = getChildObj(objData1,xAxisVal);
					}
					if (objData1.get("name").equals(xAxisVal) && !objData1.isEmpty()) {//!objData1.isEmpty()
						if(objData1.containsKey("children")) {
							objData1.put("children", childArr);
						} else {
							objData1.put("children", subChildArr);
						}
//                		objData1.put("children", subChildArr);
						break; // Stop the loop once the name is xAxisVal
					}
				}
			}
			//							                break;
		}
		return dataArr;
	}
}
