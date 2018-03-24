package vworld;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

public class Building3dCrawlerPublish {
	
	static int nn = 0;
	static int nnP = 0;
	
	static String url3 = "http://xdworld.vworld.kr:8080/XDServer/requestLayerNode?APIKey=";	
	static String url4 = "http://xdworld.vworld.kr:8080/XDServer/requestLayerObject?APIKey=";	
	static String apiKey = "----apikey�� ��û�Ͽ� ���� �� �̰��� �����Ѵ�.-----";
	static String referer = "http://localhost:4141"; //apikey�� ��û�� �� �Է��ϴ� ȣ��Ʈ �ּ�
	
	static String storageFolder  = "x:\\vworld\\";	 // �ѹ� ������ ��� ������ �� ����
	static String targetFolder = "x:\\vworld\\#obj_sample\\"; //�׶��׶� �ʿ��� ������ ������ ����, ��û ������ �޶��������� �ٲپ��൵ ���� ��û ���ϴ� ����.
	
	static String csName1 = "EPSG:4326";
	static String csName2 = "EPSG:5179";	    
	static CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
	static CRSFactory csFactory = new CRSFactory();	    
	static CoordinateReferenceSystem crs1 = csFactory.createFromName(csName1);
	static CoordinateReferenceSystem crs2 = csFactory.createFromName(csName2);
	static CoordinateTransform trans = ctFactory.createTransform(crs1, crs2);
	static ProjCoordinate p1 = new ProjCoordinate();
	static ProjCoordinate p2 = new ProjCoordinate();
	
	//������ ���� 14���� �޾ƿ;� �Ѵ�.
	//static String layerName = "facility_bridge";
	//static int level = 14;
	
	//�ǹ��� ���� 15���� �޾ƿ;� �Ѵ�.
	static String layerName = "facility_build";
	static int level = 15;	
	
	static double unit = 360 / (Math.pow(2, level) * 10); //15������ ���� ũ��(����:������)
	
	static HashSet<String> jpgList;
	static HashSet<String> fileNamesXdo;
	
	private static String[] getCoordination() {		
		
		String minmax = "37.560639, 126.991816,37.571219, 126.999605"; //sample ��ǥ
		String[] temp1 = minmax.replaceAll(" ", "").split(",");
		return new String[]{temp1[1],temp1[0], temp1[3],temp1[2]};
	}

	public static void main(String[] args) throws IOException {	
		
		//�ʿ��� subfolder�� �����. �̹� ������ �ǳʶڴ�.
		String[] folders1 = {"jpg","xdo_dat","xdo_Files","xdo_List",};
		makeSubFolders(storageFolder, folders1);
		String[] folders2 = {"xdo_obj","xdo_obj_UTMK"};
		makeSubFolders(targetFolder,folders2);
		
		//���ʿ��� �ߺ����� �ٿ�ε带 ���� �ʱ� ���� ������ �޾Ƴ��Ҵ� ���� ����� �о���δ�. 
		HashSet<String> fileNamesDAT = getFileNames(storageFolder+"xdo_dat\\", ".dat");
		HashSet<String> fileNamesXdoList = getFileNames(storageFolder+"xdo_List\\", ".txt");
		jpgList = getFileNames(storageFolder+"jpg\\", ".jpg");
		fileNamesXdo = getFileNames(storageFolder+"xdo_Files\\", ".xdo");
		
		//�տ��� ������ ���� ���� ��ǥ���� �޾ƿ´�.
		String[] latlon = getCoordination();
		String minLon = latlon[0];   //�浵
		String minLat = latlon[1];	 // ����
		String maxLon = latlon[2];
		String maxLat = latlon[3];		
		
		//������ request�� response�� ���� idx idy ��ϵ��� �޾ƿ;� ������, ������ ����� ���� ���� �� �����Ƿ� ���� �Ѵ�.
		int minIdx = (int)Math.floor((Double.parseDouble(minLon)+180)/unit);
		int minIdy = (int)Math.floor((Double.parseDouble(minLat)+90)/unit);
		int maxIdx = (int)Math.floor((Double.parseDouble(maxLon)+180)/unit);
		int maxIdy = (int)Math.floor((Double.parseDouble(maxLat)+90)/unit);
		System.out.println(minIdx+" , "+minIdy+" | "+maxIdx+" , "+maxIdy);
		
		String[][] idxIdyList = new String[(maxIdx-minIdx+1)*(maxIdy-minIdy+1)][2];
		int index = 0;
		for (int i=minIdx ; i<=maxIdx ; i++) {
			for (int j=minIdy ; j<=maxIdy; j++) {
				idxIdyList[index][0] = i+"";
				idxIdyList[index][1] = j+"";
				index++;
			}
		}
		
		
		L1 : for (int i=0; i<idxIdyList.length ; i++) {
			
			System.out.println("file :"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+"���� ����....."+(i+1)+"/"+idxIdyList.length);
			
			//request�� ���� �֤� ���� 
			String address3 = url3 + apiKey +"&Layer=" + layerName + "&Level=" + level 
					+ "&IDX=" + idxIdyList[i][0] + "&IDY=" + idxIdyList[i][1];
			String fileNameXdo = "xdoList"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+".dat";
			
			//IDX�� IDY �� nodeLevel�� ������ xdo��ϵ��� �޾� dat�� �����Ѵ�.
			if (!fileNamesDAT.contains(fileNameXdo)) {
				sendQueryForBin(address3, storageFolder+"xdo_dat\\"+fileNameXdo);   			
			}
			
			//������ ���� ������ �ǹ����� ���� ��� datChecker�� false�� ��ȯ�Ѵ�. �̶� �ش� ������ �ǳʶڴ�.
			//������ �ϴ� dat ������ ������ ��, datChecker���� �ٽ� �о�´�. 
			if (!datChecker(storageFolder+"xdo_dat\\"+fileNameXdo)) {
				System.out.println("�ڷ� ����. �ǳʶ�");
				continue L1; 			
			}
			
			String fileNameParsedXdo = "xdoList_parsed"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+".txt";			
			
			if (!fileNamesXdoList.contains(fileNameParsedXdo)) {
				datParser(storageFolder+"xdo_dat\\"+fileNameXdo, storageFolder+"xdo_List\\"+fileNameParsedXdo); //dat�� �ٽ� �а� txt�� �Ľ��Ѵ�.
			}
			
			//obj�� �ְų� ���ų� �׳� �����Ѵ�. ������ dat�� ������ ������ ���̰� obj�� �� ��ǻ�Ϳ��� �ణ�� ��길 �ϸ� �Ǳ� �����̴�. ��� ������ ������ �����ϸ� target�� �ԾƳ��ش�.
			//�ؽ��ĸ� ������ ���� obj�� mtl�� ����Ѵ�.
			String fileNameObj = "final_object file"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+".obj";
			String fileNameMtl = "final_object file"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+".mtl";
			
			System.out.println(fileNameObj+"����õ�....."+(i+1)+"/"+idxIdyList.length);			
			xdosToObj(fileNameParsedXdo, fileNameObj, fileNameMtl ,idxIdyList[i][0],idxIdyList[i][1] ); //�������� xdo���� ȣ���Ͽ� obj ���Ϸ� �����.			
			System.out.println(fileNameObj+"����Ϸ�....."+(i+1)+"/"+idxIdyList.length);
			nn=0;
			nnP=0;
		}
		

	}
	
	


	
	private static void xdosToObj(String fileName, String fileNameObj, String fileNameMtl ,String nodeIDX, String nodeIDY) throws IOException {
		
		//�����غ�
		FileWriter fw = new FileWriter(targetFolder+"xdo_obj\\"+fileNameObj);
		BufferedWriter bw = new BufferedWriter(fw);
		
		bw.write("# Rhino");
		bw.newLine();
		bw.newLine();
		bw.write("mtllib "+fileNameMtl);
		bw.newLine();
		
		FileWriter fw1 = new FileWriter(targetFolder+"xdo_obj_UTMK\\"+fileNameObj);
		BufferedWriter bw1 = new BufferedWriter(fw1);
		
		bw1.write("# Rhino");
		bw1.newLine();
		bw1.newLine();
		bw1.write("mtllib "+fileNameMtl);
		bw1.newLine();
		
		FileWriter fwm = new FileWriter(targetFolder+"xdo_obj\\"+fileNameMtl);
		BufferedWriter bwm = new BufferedWriter(fwm);

		FileWriter fwm1 = new FileWriter(targetFolder+"xdo_obj_UTMK\\"+fileNameMtl);
		BufferedWriter bwm1 = new BufferedWriter(fwm1);		
		
		
		//�б�
		FileReader fr = new FileReader(storageFolder+"xdo_List\\"+fileName);
		BufferedReader br = new BufferedReader(fr);
		
		String line;
		String[] temp;
		
		//�� ���� ���ϸ���� �ƴϹǷ� �ǳʶڴ�.
		line=br.readLine();
		line=br.readLine();
		line=br.readLine();
		line=br.readLine();
		
		
		//xdoList���� xdo �����̸��� �ϳ��ϳ� �о���̸鼭 obj������ ����Ѵ�.
		//�ϳ��� xdoList�� �ִ� �ǹ����� �ϳ��� obj���Ͽ� �ִ´�.
		while ((line=br.readLine()) != null) {
			
			temp = line.split("\\|");
			String version = temp[0].split("\\.")[3];
			
			String xdofileName = temp[15];
			double lon = Double.parseDouble(temp[4]);
			double lat = Double.parseDouble(temp[5]);
			//float altitude = Float.parseFloat(temp[6]);
			
			//xdo ������ 3.0.0.1 ������ 3.0.0.2 ������ �ִ�. ������ ���Ͽ� ���� ������ ���� ����� �ٸ��Ƿ� �����Ͽ� ó���Ѵ�.
			if(version.equals("1")) {
				
				//������ �����ϴ� xdo�����̸� �ٽ� ��û���� �ʴ´�.
				if (!fileNamesXdo.contains(xdofileName)) {
					sendQueryForBin(getAddressForXdoFile(xdofileName, nodeIDX, nodeIDY), storageFolder+"xdo_Files\\"+xdofileName);
				}
				//System.out.println("version1");
				//�ձ� ���� ���� ��� ���� �� ���� ���·� obj�� �����.
				xdo31Parser(xdofileName, bw, getAddressForJpgFile("", nodeIDX, nodeIDY), bwm);
				xdo31Parser_planar(xdofileName, bw1, lon, lat, bwm1);			
				
			} else if (version.equals("2")){
				
				//������ �����ϴ� xdo�����̸� �ٽ� ��û���� �ʴ´�.
				if (!fileNamesXdo.contains(xdofileName)) {
					sendQueryForBin(getAddressForXdoFile(xdofileName, nodeIDX, nodeIDY), storageFolder+"xdo_Files\\"+xdofileName); 
				}
				//System.out.println("version2");
				//�ձ� ���� ���� ��� ���� �� ���� ���·� obj�� �����.
				xdo32Parser(xdofileName, bw, getAddressForJpgFile("", nodeIDX, nodeIDY), bwm);// �ٽ� xdo ������ �о �Ľ��� �� �����Ѵ�.
				xdo32Parser_planar(xdofileName, bw1, lon, lat, bwm1);										
			}
			
		}		
		bw.close();
		bw1.close();
		bwm.close();
		bwm1.close();
		br.close();		
	}
	

	private static void xdo31Parser(String fileName, BufferedWriter bw, String queryAddrForJpg, BufferedWriter bwm) throws IOException {
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(storageFolder+"xdo_Files\\"+fileName)));
		
		int type = pU8(bis);
		int objectId = pU32(bis);
		int keyLen = pU8(bis);
		String key = pChar(bis,keyLen);
		double[] objectBox = {pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis)};
		float altitude = pFloat(bis);
		
		double objX = (objectBox[0]+objectBox[3])/2;
		double objY = (objectBox[1]+objectBox[4])/2;
		double objZ = (objectBox[2]+objectBox[5])/2;
		
		int vertexCount = pU32(bis);		
		
		double[][] vertex = new double[vertexCount][8];
		
		for (int i =0 ; i<vertexCount ; i++) {
			
			float vx = pFloat(bis);
			float vy = pFloat(bis);
			float vz = pFloat(bis);
			float vnx = pFloat(bis);
			float vny = pFloat(bis);
			float vnz = pFloat(bis);
			float vtu = pFloat(bis);
			float vtv = pFloat(bis);			
			
			vertex[i][0] = objX + vx;
			vertex[i][1] = -1 * (objY+ vy);
			vertex[i][2] = objZ + vz;
			vertex[i][3] = vnx;
			vertex[i][4] = vny;
			vertex[i][5] = vnz;
			vertex[i][6] = vtu;
			vertex[i][7] = (1.0f-vtv);
			
		}
		
		int indexedNumber = pU32(bis);
		
		short[] indexed = new short[indexedNumber];
		for (int i=0; i<indexedNumber ; i++) {			
			indexed[i] = (short) (pU16(bis)+1);
		}
		
		int colorA = pU8(bis);
		int colorR = pU8(bis);
		int colorG = pU8(bis);
		int colorB = pU8(bis);		
		
		int imageLevel = pU8(bis);
		int imageNameLen = pU8(bis);
		String imageName = pChar(bis, imageNameLen);		
		
		int nailSize = pU32(bis);		
		//writeNailData(bis, imageName,nailSize);
		
		if (!jpgList.contains(imageName)) sendQueryForBin(queryAddrForJpg+imageName, storageFolder+"jpg\\"+imageName);
		//������ҿ� �ִ� �ؽ��� ������ obj�� ���� ���� �������ش�.
		fileCopy(storageFolder+"jpg\\"+imageName, targetFolder+"xdo_obj\\"+imageName);
		
		bw.write("g "+key);
		bw.newLine();
		
		//material�� �⺻�� �Ӽ��� ���Ƿ� �Ʒ��� ���� ����.
		//mtl ������ �ڼ��� ������ �Ʒ��� ����
		//http://paulbourke.net/dataformats/mtl/
		bwm.write("newmtl "+key);			
		bwm.newLine();
		bwm.write("Ka 1.000000 1.000000 1.000000");	
		bwm.newLine();
		bwm.write("Kd 1.000000 1.000000 1.000000");	
		bwm.newLine();
		bwm.write("Ks 0.000000 0.000000 0.000000");	
		bwm.newLine();
		bwm.write("Tr 1.000000");	
		bwm.newLine();
		bwm.write("illum 1");
		bwm.newLine();
		bwm.write("Ns 0.000000");	
		bwm.newLine();
		bwm.write("map_Kd "+imageName);
		bwm.newLine();
		bwm.newLine();		

		for (int i=0 ; i<vertexCount ; i++) {				
			bw.write("v "+vertex[i][0]+" "+vertex[i][1]+" "+vertex[i][2]);	
			bw.newLine();
		}
		for (int i=0 ; i<vertexCount ; i++) {				
			bw.write("vt "+vertex[i][6]+" "+vertex[i][7]);	
			bw.newLine();
		}
		for (int i=0 ; i<vertexCount ; i++) {				
			bw.write("vn "+vertex[i][3]+" "+vertex[i][4]+" "+vertex[i][5]);	
			bw.newLine();
		}
		bw.write("usemtl "+key);
		bw.newLine();
		for (int i=0 ; i<indexedNumber ; i=i+3) {
			bw.write("f ");
			bw.write((indexed[i]+nnP)+"/"+(indexed[i]+nnP)+"/"+(indexed[i]+nnP)+" ");
			bw.write((indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+" ");	
			bw.write((indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP));	
			bw.newLine();
		}
		nn=nn+indexedNumber;
		bis.close();	
	}
	
	private static void xdo31Parser_planar(String fileName, BufferedWriter bw, double lon, double lat, BufferedWriter bwm) throws IOException {
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(storageFolder+"xdo_Files\\"+fileName)));		
		
		int type = pU8(bis);
		int objectId = pU32(bis);
		int keyLen = pU8(bis);
		String key = pChar(bis,keyLen);
		double[] objectBox = {pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis)};
		float altitude = pFloat(bis);
		
		double objX = (objectBox[0]+objectBox[3])/2;
		double objY = (objectBox[1]+objectBox[4])/2;
		double objZ = (objectBox[2]+objectBox[5])/2;
		
		float[] objxyz = rotate3d((float)objX, (float)objY, (float)objZ, lon, lat);
		
		p1.x = lon;
	    p1.y = lat;	
	    trans.transform(p1, p2);		
		
		int vertexCount = pU32(bis);	
		
		double[][] vertex = new double[vertexCount][8];
		
		for (int i =0 ; i<vertexCount ; i++) {
			
			float vx = pFloat(bis);
			float vy = pFloat(bis);
			float vz = pFloat(bis);
			float vnx = pFloat(bis);
			float vny = pFloat(bis);
			float vnz = pFloat(bis);
			float vtu = pFloat(bis);
			float vtv = pFloat(bis);
			
			float[] xyz = rotate3d(vx, vy, vz, lon, lat);			
			
			vertex[i][0] = p2.x + xyz[0];
			vertex[i][1] = p2.y -1 * (xyz[1]);			
			vertex[i][2] = xyz[2] +objxyz[2] -6378137; //vworld�� �����ϰ� �ִ� world wind�� Ÿ��ü�� �ƴ϶� 6,378,137m�� �������� ������ ����ü��.
			vertex[i][3] = vnx;
			vertex[i][4] = vny;
			vertex[i][5] = vnz;
			vertex[i][6] = vtu;
			vertex[i][7] = (1.0f-vtv);
			
		}
		
		int indexedNumber = pU32(bis);		
		
		short[] indexed = new short[indexedNumber];
		for (int i=0; i<indexedNumber ; i++) {
			indexed[i] = (short) (pU16(bis)+1);		
		}		

		int colorA = pU8(bis);
		int colorR = pU8(bis);
		int colorG = pU8(bis);
		int colorB = pU8(bis);		
		
		int imageLevel = pU8(bis);
		int imageNameLen = pU8(bis);
		String imageName = pChar(bis, imageNameLen);		
		
		int nailSize = pU32(bis);		
		//writeNailData(bis, imageName,nailSize);
		//������ҿ� �ִ� �ؽ��� ������ obj�� ���� ���� �������ش�.
		fileCopy(storageFolder+"jpg\\"+imageName, targetFolder+"xdo_obj_UTMK\\"+imageName);
		
		bw.write("g "+key);
		bw.newLine();
		
		bwm.write("newmtl "+key);
		bwm.newLine();
		bwm.write("Ka 1.000000 1.000000 1.000000");	
		bwm.newLine();
		bwm.write("Kd 1.000000 1.000000 1.000000");	
		bwm.newLine();
		bwm.write("Ks 0.000000 0.000000 0.000000");	
		bwm.newLine();
		bwm.write("Tr 1.000000");	
		bwm.newLine();
		bwm.write("illum 1");
		bwm.newLine();
		bwm.write("Ns 0.000000");	
		bwm.newLine();
		bwm.write("map_Kd "+imageName);
		bwm.newLine();
		bwm.newLine();
		
		for (int i=0 ; i<vertexCount ; i++) {				
			bw.write("v "+vertex[i][0]+" "+vertex[i][1]+" "+vertex[i][2]);	
			bw.newLine();
		}
		for (int i=0 ; i<vertexCount ; i++) {				
			bw.write("vt "+vertex[i][6]+" "+vertex[i][7]);	
			bw.newLine();
		}
		for (int i=0 ; i<vertexCount ; i++) {				
			bw.write("vn "+vertex[i][3]+" "+vertex[i][4]+" "+vertex[i][5]);	
			bw.newLine();
		}
		
		bw.write("usemtl "+key);
		bw.newLine();
		
		for (int i=0 ; i<indexedNumber ; i=i+3) {
			bw.write("f ");
			bw.write((indexed[i]+nnP)+"/"+(indexed[i]+nnP)+"/"+(indexed[i]+nnP)+" ");
			bw.write((indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+" ");	
			bw.write((indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP));	
			bw.newLine();
		}
		nnP=nnP+indexedNumber;
		bis.close();	
		
	}

	private static void xdo32Parser(String fileName, BufferedWriter bw, String queryAddrForJpg, BufferedWriter bwm) throws IOException {
		
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(storageFolder+"xdo_Files\\"+fileName)));
		

		int type = pU8(bis);
		int objectId = pU32(bis);
		int keyLen = pU8(bis);
		String key = pChar(bis,keyLen);
		double[] objectBox = {pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis)};
		float altitude = pFloat(bis);		
		
		double objX = (objectBox[0]+objectBox[3])/2;
		double objY = (objectBox[1]+objectBox[4])/2;
		double objZ = (objectBox[2]+objectBox[5])/2;
		
		int faceNum = pU8(bis);
		
		for (int j=0 ; j<faceNum ; j++) {
			
			int vertexCount = pU32(bis);			
			
			double[][] vertex = new double[vertexCount][8];
			
			for (int i =0 ; i<vertexCount ; i++) {
				
				float vx = pFloat(bis);
				float vy = pFloat(bis);
				float vz = pFloat(bis);
				float vnx = pFloat(bis);
				float vny = pFloat(bis);
				float vnz = pFloat(bis);
				float vtu = pFloat(bis);
				float vtv = pFloat(bis);
				
				vertex[i][0] = objX + vx;
				vertex[i][1] = -1 * (objY+ vy);				
				vertex[i][2] = objZ + vz;
				vertex[i][3] = vnx;
				vertex[i][4] = vny;
				vertex[i][5] = vnz;
				vertex[i][6] = vtu;
				vertex[i][7] = (1.0f-vtv);				
				
			}
			
			int indexedNumber = pU32(bis);			

			short[] indexed = new short[indexedNumber];
			for (int i=0; i<indexedNumber ; i++) {				
				indexed[i] = (short) (pU16(bis)+1);				
			}
			
			int colorA = pU8(bis);
			int colorR = pU8(bis);
			int colorG = pU8(bis);
			int colorB = pU8(bis);			
			
			int imageLevel = pU8(bis);
			int imageNameLen = pU8(bis);
			String imageName = pChar(bis, imageNameLen);			
			
			int nailSize = pU32(bis);
			
			//writeNailData(bis, imageName,nailSize);
			if (!jpgList.contains(imageName)) sendQueryForBin(queryAddrForJpg+imageName, storageFolder+"jpg\\"+imageName);
			//������ҿ� �ִ� �ؽ��� ������ obj�� ���� ���� �������ش�.
			fileCopy(storageFolder+"jpg\\"+imageName, targetFolder+"xdo_obj\\"+imageName);
			
			bw.write("g "+key);
			bw.newLine();
			
			//material�� �⺻�� �Ӽ��� ���Ƿ� �Ʒ��� ���� ����.
			//mtl ������ �ڼ��� ������ �Ʒ��� ����
			//http://paulbourke.net/dataformats/mtl/
			bwm.write("newmtl "+key);			
			bwm.newLine();
			bwm.write("Ka 1.000000 1.000000 1.000000");	
			bwm.newLine();
			bwm.write("Kd 1.000000 1.000000 1.000000");	
			bwm.newLine();
			bwm.write("Ks 0.000000 0.000000 0.000000");	
			bwm.newLine();
			bwm.write("Tr 1.000000");	
			bwm.newLine();
			bwm.write("illum 1");
			bwm.newLine();
			bwm.write("Ns 0.000000");	
			bwm.newLine();
			bwm.write("map_Kd "+imageName);
			bwm.newLine();
			bwm.newLine();
			
			for (int i=0 ; i<vertexCount ; i++) {				
				bw.write("v "+vertex[i][0]+" "+vertex[i][1]+" "+vertex[i][2]);	
				bw.newLine();
			}
			for (int i=0 ; i<vertexCount ; i++) {				
				bw.write("vt "+vertex[i][6]+" "+vertex[i][7]);	
				bw.newLine();
			}
			for (int i=0 ; i<vertexCount ; i++) {				
				bw.write("vn "+vertex[i][3]+" "+vertex[i][4]+" "+vertex[i][5]);	
				bw.newLine();
			}
			
			bw.write("usemtl "+key);
			bw.newLine();
			
			for (int i=0 ; i<indexedNumber ; i=i+3) {
				bw.write("f ");
				bw.write((indexed[i]+nnP)+"/"+(indexed[i]+nnP)+"/"+(indexed[i]+nnP)+" ");
				bw.write((indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+" ");	
				bw.write((indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP));	
				bw.newLine();
			}
				
			nn=nn+indexedNumber;	
			
		}
		bis.close();		
	
		
	}

	private static void xdo32Parser_planar(String fileName, BufferedWriter bw, double lon, double lat, BufferedWriter bwm) throws IOException {
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(storageFolder+"xdo_Files\\"+fileName)));		
		
		int type = pU8(bis);
		int objectId = pU32(bis);
		int keyLen = pU8(bis);
		String key = pChar(bis,keyLen);
		double[] objectBox = {pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis)};
		float altitude = pFloat(bis);	
		
		double objX = (objectBox[0]+objectBox[3])/2;
		double objY = (objectBox[1]+objectBox[4])/2;
		double objZ = (objectBox[2]+objectBox[5])/2;
		
		float[] objxyz = rotate3d((float)objX, (float)objY, (float)objZ, lon, lat);
		
		p1.x = lon;
	    p1.y = lat;	
	    trans.transform(p1, p2);
		
		int faceNum = pU8(bis);		
		
		for (int j=0 ; j<faceNum ; j++) {

			int vertexCount = pU32(bis);			
			
			double[][] vertex = new double[vertexCount][8];
			
			for (int i =0 ; i<vertexCount ; i++) {
				
				float vx = pFloat(bis);
				float vy = pFloat(bis);
				float vz = pFloat(bis);
				float vnx = pFloat(bis);
				float vny = pFloat(bis);
				float vnz = pFloat(bis);
				float vtu = pFloat(bis);
				float vtv = pFloat(bis);
				
				float[] xyz = rotate3d(vx, vy, vz, lon, lat);
				
				vertex[i][0] = p2.x + xyz[0];
				vertex[i][1] = p2.y -1 * (xyz[1]);
				vertex[i][2] = xyz[2] +objxyz[2] -6378137;
				vertex[i][3] = vnx;
				vertex[i][4] = vny;
				vertex[i][5] = vnz;
				vertex[i][6] = vtu;
				vertex[i][7] = (1.0f-vtv);				
				
			}
			
			int indexedNumber = pU32(bis);

			short[] indexed = new short[indexedNumber];
			for (int i=0; i<indexedNumber ; i++) {				
				indexed[i] = (short) (pU16(bis)+1);							
			}
			
			int colorA = pU8(bis);
			int colorR = pU8(bis);
			int colorG = pU8(bis);
			int colorB = pU8(bis);
			
			int imageLevel = pU8(bis);
			int imageNameLen = pU8(bis);
			String imageName = pChar(bis, imageNameLen);
			
			int nailSize = pU32(bis);
			
			//writeNailData(bis, imageName,nailSize);
			//������ҿ� �ִ� �ؽ��� ������ obj�� ���� ���� �������ش�.
			fileCopy(storageFolder+"jpg\\"+imageName, targetFolder+"xdo_obj_UTMK\\"+imageName);
			
			bw.write("g "+key);
			bw.newLine();
			
			bwm.write("newmtl "+key);
			bwm.newLine();
			bwm.write("Ka 1.000000 1.000000 1.000000");	
			bwm.newLine();
			bwm.write("Kd 1.000000 1.000000 1.000000");	
			bwm.newLine();
			bwm.write("Ks 0.000000 0.000000 0.000000");	
			bwm.newLine();
			bwm.write("Tr 1.000000");	
			bwm.newLine();
			bwm.write("illum 1");
			bwm.newLine();
			bwm.write("Ns 0.000000");	
			bwm.newLine();
			bwm.write("map_Kd "+imageName);
			bwm.newLine();
			bwm.newLine();
			
			for (int i=0 ; i<vertexCount ; i++) {				
				bw.write("v "+vertex[i][0]+" "+vertex[i][1]+" "+vertex[i][2]);	
				bw.newLine();
			}
			
			for (int i=0 ; i<vertexCount ; i++) {				
				bw.write("vt "+vertex[i][6]+" "+vertex[i][7]);	
				bw.newLine();
			}
			for (int i=0 ; i<vertexCount ; i++) {				
				bw.write("vn "+vertex[i][3]+" "+vertex[i][4]+" "+vertex[i][5]);	
				bw.newLine();
			}
			
			bw.write("usemtl "+key);
			bw.newLine();
			
			for (int i=0 ; i<indexedNumber ; i=i+3) {
				bw.write("f ");
				bw.write((indexed[i]+nnP)+"/"+(indexed[i]+nnP)+"/"+(indexed[i]+nnP)+" ");
				bw.write((indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+"/"+(indexed[i+1]+nnP)+" ");	
				bw.write((indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP)+"/"+(indexed[i+2]+nnP));	
				bw.newLine();
			}
				
			nnP=nnP+indexedNumber;				
			
		}
		bis.close();	
		
	}


	//xdo �� �⺻������ ���Ե� ������ �ػ� �ؽ��� ������ ������.
	private static void writeNailData(BufferedInputStream bis, String fileName, int nailSize) throws IOException {
		
		byte[] b = new byte[nailSize];
		int readByteNo = bis.read(b);
		
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(storageFolder+"xdo_Files\\"+fileName)));
		
		bos.write(b);
		bos.close();
		return;
	}
	

	private static boolean datChecker(String fileNameXdo) throws IOException {
		
		FileReader fr = new FileReader(fileNameXdo);
		BufferedReader br = new BufferedReader(fr);
		
		//ù�ٿ� �ش� ������ �ִ�.
		String line = br.readLine();		
		br.close();		
		int check = line.indexOf("ERROR_SERVICE_FILE_NOTTHING");
		
		if (check==-1) return true;
		else return false;	
		
	}



	/**
	 * xdo ����Ʈ�� �ִ� dat�� �а� txt�� �Ľ��Ͽ� �����Ѵ�.
	 * @param fileName
	 * @param fileNameW
	 * @throws IOException
	 */
	private static void datParser(String fileName, String fileNameW) throws IOException {
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(fileName)));		
		
		FileWriter fw = new FileWriter(fileNameW);
		BufferedWriter bw = new BufferedWriter(fw);
		
		int[] datHeader = new int[4];
		String[] datHeaderName = {"level","IDX","IDY","ObjectCount"};
		
		//Header �б�
		for (int i=0 ; i<4 ; i++) {
			datHeader[i] = pU32(bis);
			bw.write(datHeaderName[i]+"="+datHeader[i]);
			bw.newLine();
		}
		
		//Real3D Model Object �б�
		for (int i=0 ; i<datHeader[3] ; i++) {
			
			String r_version = pU8(bis)+"."+pU8(bis)+"."+pU8(bis)+"."+pU8(bis);
			int r_type = pU8(bis);
			int r_keylen = pU8(bis);
			
			String r_key = pChar(bis,r_keylen);
			
			double[] r_CenterPos = {pDouble(bis),pDouble(bis)};	
			
			float r_altitude = pFloat(bis);
			
			double[] r_box = {pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis),pDouble(bis)};			
			
			int r_imgLevel = pU8(bis);
			int r_dataFileLen = pU8(bis);
			String r_dataFile = pChar(bis,r_dataFileLen);
			
			int r_imgFileNameLen = pU8(bis);
			String r_imgFileName = pChar(bis,r_imgFileNameLen);			
			
			bw.write(r_version+"|"+r_type+"|"+r_keylen+"|"+r_key+"|"+r_CenterPos[0]+"|"+r_CenterPos[1]
					+"|"+r_altitude+"|"+r_box[0]+"|"+r_box[1]+"|"+r_box[2]+"|"+r_box[3]+"|"+r_box[4]+"|"+r_box[5]+"|"
					+r_imgLevel+"|"+r_dataFileLen+"|"+r_dataFile+"|"+r_imgFileNameLen+"|"+r_imgFileName);
			bw.newLine();
		}
		bis.close();
		bw.close();
		
	}


	//���̳ʸ� ���� �Ľ�
	private static String pVersion(BufferedInputStream bis) throws IOException {
		
		byte[] b = new byte[1];
		int readByteNo = bis.read(b);
		
		return null;
	}

	//���̳ʸ� ���� �Ľ�
	private static float pFloat(BufferedInputStream bis) throws IOException {
		
		byte[] b = new byte[4];
		int readByteNo = bis.read(b);	
		ArrayUtils.reverse(b);
		return ByteBuffer.wrap(b).getFloat();
		
	}

	//���̳ʸ� ���� �Ľ�
	private static double pDouble(BufferedInputStream bis) throws IOException {
		
		byte[] b = new byte[8];
		int readByteNo = bis.read(b);	
		ArrayUtils.reverse(b);
		return ByteBuffer.wrap(b).getDouble();
		
	}

	//���̳ʸ� ���� �Ľ�
	private static String pChar(BufferedInputStream bis, int r_keylen) throws IOException {
		
		StringBuffer string = new StringBuffer();
		
		for (int i = 0 ; i<r_keylen ; i++) {
			
			byte[] b = new byte[1];
			int readByteNo = bis.read(b);				
			char cha = (char)b[0];
			string.append(cha);
		}
		
		return string.toString();
		
	}

	//���̳ʸ� ���� �Ľ�
	private static int pU8(BufferedInputStream bis) throws IOException {
		
		byte[] b = new byte[1];
		int readByteNo = bis.read(b);		
		int number = b[0];		
		return number;
		
	}
	
	//���̳ʸ� ���� �Ľ�
	private static short pU16(BufferedInputStream bis) throws IOException {
		
		byte[] b = new byte[2];
		int readByteNo = bis.read(b);
		ArrayUtils.reverse(b);
		return ByteBuffer.wrap(b).getShort();
		
	}

	//���̳ʸ� ���� �Ľ�
	private static int pU32(BufferedInputStream bis) throws IOException {
		
		
		byte[] b = new byte[4];
		int readByteNo = bis.read(b);
		ArrayUtils.reverse(b);
		return ByteBuffer.wrap(b).getInt();		
	}

	
	private static float[] rotate3d(float vx, float vy, float vz, double lon, double lat) {
		
		float x,y,z;
		
		double p = (lon)/180 * Math.PI;
		double t = (90-lat)/180 * Math.PI;
		
		//���� ȸ�����Ĵ�� �ϴϱ� 90�� ȸ���� ����� ���� z���� �߽����� �ٽ� -90�� ȸ���� �ߴ�.
		y = (float) (Math.cos(t)*Math.cos(p)*vx - Math.cos(t)* Math.sin(p) * vy - Math.sin(t)*vz);
		x = -1 *(float) (Math.sin(p)*vx + Math.cos(p)*vy);
		z = (float) (Math.sin(t)*Math.cos(p)*vx -Math.sin(t)*Math.sin(p)*vy + Math.cos(t)*vz);		
		
		return new float[]{x,y,z};
	}
	
	private static void makeSubFolders(String fileLocation, String[] subfolders) {
		
		File file = new File(fileLocation);
		String[] files = file.list(); 
		
		for (String subfolder : subfolders) {
			
			boolean isExist = false;
			
			if (files != null ) {
				for (int j = 0; j < files.length; j++) { 
					if(files[j].equals(subfolder)) {
						isExist = true; //���� �̸��� ������ ����������
						break;
					}		
	            } //for
			}			
			if (!isExist) {				
				File newDir = new File(fileLocation+subfolder);					
				newDir.mkdirs();		
			}
		}
	}


	
	/**
	 * httpRequest�� ������ ���̳ʸ� ������ �޾� �����Ѵ�.
	 * @param address
	 * @param xdofileName
	 */
	private static void sendQueryForBin(String address, String fileName) {

		try {			
			//�� �κ��� ������ ����ġ���� httpClient�� �ٿ�޾� ��ġ�Ͽ��� �Ѵ�.
			CloseableHttpClient httpClient = HttpClients.createDefault();
			
			HttpGet httpGet = new HttpGet(address);	
			httpGet.addHeader("Referer",referer); //api key ��û�� ����� �ּҸ� ���ش�.
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();	
			BufferedInputStream bis = null;
			
			if (entity != null) {
			    long len = entity.getContentLength();
			    bis = new BufferedInputStream(entity.getContent());			    
			}
			
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
			int inByte;
			while((inByte = bis.read()) != -1) bos.write(inByte);
			
			bis.close();
			bos.close();			
			httpResponse.close();
			
		} catch(Exception e) {
			e.printStackTrace();
		}		
		return;		
	}
	

	private static HashSet<String> getFileNames(String fileLocation, String extension) {
		
		HashSet<String> fileNames = new HashSet<String>(); 
		
		File[] files = (new File(fileLocation)).listFiles(); 
		
		// ���丮�� ��� ���� �ʴٸ� 
		if(!(files.length <= 0)){ 
			
			for (int i = 0; i < files.length; i++) { 
				
				// ���丮�� �ƴ� ������ ��쿡�� txt �������� �˻��Ѵ�. 
				if(files[i].isFile() && files[i].getName().endsWith(extension)){ 

				fileNames.add(files[i].getName());
				//System.out.println(files[i].getName()+extension);

            } 

         } 

       }		
		
		return fileNames;
	}


	private static String getAddressForXdoFile(String dataFile, String nodeIDX, String nodeIDY) {		
		
		String address= url4+ apiKey + "&Layer=" + layerName+"&Level="+ level +"&IDX=" +nodeIDX+"&IDY="+ nodeIDY
				+ "&DataFile="+dataFile;		
		return address;
	}
	
	private static String getAddressForJpgFile(String jpgFile, String nodeIDX, String nodeIDY) {		
		String address= url4+ apiKey + "&Layer=" + layerName+"&Level="+ level +"&IDX=" +nodeIDX+"&IDY="+ nodeIDY
				+ "&DataFile="+jpgFile;		
		return address;
	}


	private static void fileCopy(String inFileName, String outFileName) throws IOException {
		
		//http://fruitdev.tistory.com/87 	
		FileInputStream inputStream = new FileInputStream(inFileName);        
		FileOutputStream outputStream = new FileOutputStream(outFileName);
		  
		FileChannel fcin =  inputStream.getChannel();
		FileChannel fcout = outputStream.getChannel();
		  
		long size = fcin.size();
		fcin.transferTo(0, size, fcout);
		  
		fcout.close();
		fcin.close();		  
		outputStream.close();
		inputStream.close();
	}
	



	
}


