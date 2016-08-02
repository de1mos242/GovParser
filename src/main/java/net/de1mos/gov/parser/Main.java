package net.de1mos.gov.parser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import au.com.bytecode.opencsv.CSVWriter;

public class Main {

	public static void main(String[] args) {
		try {
			runParse();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void runParse() throws FileNotFoundException, IOException, Exception {
		String queryString;
		String listUrlTemplate = "%D0%A1%D0%B5%D0%B2%D0%B7%D0%B0%D0%BF%D1%83%D0%BF%D1%80%D0%B0%D0%B2%D1%82%D0%BE%D0%B4%D0%BE%D1%80";
		queryString = "http://zakupki.gov.ru/epz/contract/quicksearch/search_eis.html?searchString=%s&pageNumber=%s&recordsPerPage=_10&fz44=on&contractStageList=0";
		
		List<String> contractsUrls = new ArrayList<>();
		
		FileOutputStream fileOutputStream = new FileOutputStream("yourfile.csv");
		System.out.println("write in: " + fileOutputStream.toString());
		fileOutputStream.close();
		FileWriter fileWriter = new FileWriter("yourfile.csv");
		CSVWriter writer = new CSVWriter(fileWriter, '\t');
		
		int page = 1;
		int totalCount = 0;
		while (true) {
			String listUrl = String.format(queryString, listUrlTemplate, page);
			Document doc = Jsoup.connect(listUrl).get();
			Elements rows = doc.select(".descriptTenderTd");
			if (rows.size() <= 0) {
				break;
			}

			System.out.println(String.format("page %s: %s", page, rows.size()));
			totalCount += rows.size();
			page++;
			
			for (Element row : rows) {
				Element rowLink = row.select("dt a").first();
				if (rowLink != null) {
					String contractUrl = rowLink.attr("abs:href");
					contractsUrls.add(contractUrl);
				}
			}
			
			break;
		}
		System.out.println(String.format("total %s pages and %s contracts", page - 1, totalCount));
		
		
		for (String contractUrl : contractsUrls) {
			Map<String, String> parseContractMap = parseContract(contractUrl, writer);
			System.out.println("---------------------------------------_");
			parseContractMap.entrySet().stream().forEach(entry -> {
				System.out.println(String.format("[%s]: [%s]", entry.getKey(), entry.getValue()));
			});
			
			System.out.println("---------------------------------------_");
		}
		writer.close();
	}

	private static Map<String, String> parseContract(String contractUrl, CSVWriter writer) throws Exception {
		Document doc = Jsoup.connect(contractUrl).get();
		Map<String, String> result = new HashMap<>();
		doc.select(".noticeBoxH2").stream().forEach(tabHeader -> {
			boolean isBaseInfo = tabHeader.text().equalsIgnoreCase("Общая информация");
			boolean isCustomerInfo = tabHeader.text().equalsIgnoreCase("Информация о заказчике");
			boolean isContractInfo = tabHeader.text().equalsIgnoreCase("Общие данные");	
			boolean isContractorsInfo = tabHeader.text().equalsIgnoreCase("Информация о поставщиках");
			
			Map<String, String> blockMap = new HashMap<>();
			
			if (isBaseInfo) {
				blockMap = parseUsualTable(tabHeader.nextElementSibling());
			}
			if (isCustomerInfo) {
				blockMap = parseUsualTable(tabHeader.nextElementSibling());
			}
			if (isContractInfo) {
				blockMap = parseUsualTable(tabHeader.nextElementSibling());
			}
			result.putAll(blockMap);
		});
		return result;
	}

	private static Map<String,String> parseUsualTable(Element tabBlock) {
		Map<String, String> result = new HashMap<>();
		tabBlock.select("tr").stream().forEach(tRow -> {
			Element rowHeader = tRow.select("td").first();
			Element rowValue = rowHeader.nextElementSibling();
			result.put(rowHeader.text(), rowValue.text());
		});
		return result;
	}

}
