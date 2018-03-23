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
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import gistools.GeoPoint;
import gistools.GeoTrans;

/**
 * vworld�κ��� DEM(��ġǥ�����)�� �ܾ�´�.
 * @author vuski@github
 *
 */
public class DEMCrawler {
	
	static int nn = 0;
	
	static String url3 = "http://xdworld.vworld.kr:8080/XDServer/requestLayerNode?APIKey=";
	static String apiKey = "----���⿡�� ���� ��û�ؼ� ���� apikey�� �ִ´�------";
	
	//�ߺ� �ٿ��̳� ��ȯ���� �ʵ��� ������ ����
	static String storageDirectory  = "X:\\source\\vworld_terrain\\";
	
	//����� �ϴ� ������ �׶��׶� �ٸ��� �������ָ� ����. obj���ϵ鸸 �����
	static String targetDirectory = "x:\\source\\vworld\\#obj_test\\";
	
	//�Ʒ����� �� ����
	//https://github.com/nasa/World-Wind-Java/blob/master/WorldWind/src/gov/nasa/worldwind/globes/Earth.java
	public static final double WGS84_EQUATORIAL_RADIUS = 6378137.0; // ellipsoid equatorial getRadius, in meters
    public static final double WGS84_POLAR_RADIUS = 6356752.3; // ellipsoid polar getRadius, in meters
    public static final double WGS84_ES = 0.00669437999013; // eccentricity squared, semi-major axis / �̽ɷ� ���� / �̽ɷ� = Math.sqrt(1-(��ݰ�����/�ܹݰ�����))

    public static final double ELEVATION_MIN = -11000d; // Depth of Marianas trench
    public static final double ELEVATION_MAX = 8500d; // Height of Mt. Everest.
	
	static int level = 15;
	/*
	level 15 = 1.5m grid (�뷫������)
	level 14 = 3m grid
	level 13 = 6m grid
	level 12 = 12m grid
	level 11 = 24m grid
	level 10 = 48m grid
	level 9 = 96m grid
	level 8 = 192m grid
	level 7 = 284m grid
	*/
	static double unit = 360 / (Math.pow(2, level) * 10); //15������ ���� ũ��(����:������)
	
	private static String[] getCoordination() {
		
		String minmax = "37.560639, 126.991816,37.571219, 126.999605"; //����� �ϴ� ������  {���ϴ� ����, ���ϴ� �浵, ���� ����, ���� �浵} ���� 
		String[] temp1 = minmax.replaceAll(" ", "").split(",");
		return new String[]{temp1[1],temp1[0], temp1[3],temp1[2]};
	}

	public static void main(String[] args) throws IOException {
		
		//�ʿ��� subfolder�� �����. �̹� ������ �ǳʶڴ�.
		String[] folders1 = {"DEM bil","DEM txt_Cartesian","DEM txt_latlon","DEM txt_UTMK"};
		makeSubFolders(storageDirectory, folders1);
		String[] folders2 = {"DEM obj","DEM obj_UTMK"};
		makeSubFolders(targetDirectory,folders2);
		
		String layerName = "dem";
		
		String[] latlon = getCoordination(); //� ������ �������� ���Ѵ�.
		String minLon = latlon[0]; //�浵
		String minLat = latlon[1]; //����	 
		String maxLon = latlon[2];
		String maxLat = latlon[3];
		
		//idx�� idy�� �޴� 1�ܰ� �ܰ踦 �����ϰ� ���⼭ ���� ����Ѵ�.
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
		
		//�ߺ� �ٿ�ε带 ���ϱ� ���� ���� �ִ� ���ϵ� ����� ���Ѵ�.
		HashSet<String> fileExistBil = getFileNames(storageDirectory+"DEM bil\\", ".bil");
		HashSet<String> fileExistTxt = getFileNames(storageDirectory+"DEM txt_latlon\\", ".txt");	
		HashSet<String> fileExistObj = getFileNames(targetDirectory+"DEM obj\\", ".obj");		
		
		//���� �������� �������� ó���Ѵ�.
		L1 : for (int i=0 ; i<idxIdyList.length ; i++) {
			
			System.out.println("file :"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+"���� ����....."+(i+1)+"/"+idxIdyList.length);			
			
			//���� �̹� bil ������ �����ϸ� �ǳʶڴ�.
			String fileNameBil = "terrain file_"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+".bil";			
			if (!fileExistBil.contains(fileNameBil)) { //�������� ������ �ٿ�޴´�.				
				String address3 = url3 + apiKey +"&Layer=" + layerName + "&Level=" + level 
						+ "&IDX=" + idxIdyList[i][0] + "&IDY=" + idxIdyList[i][1];				
				int size = sendQueryForBin(address3, "DEM bil\\"+fileNameBil);   //IDX�� IDY �� nodeLevel�� ������ bil��ϵ��� �޾� bil�� �����Ѵ�.
				if (size < 16900) { //����� �� ������ �ƴϸ�.
					System.out.println("file :"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+"���� �ǳʶ�(�뷮����)....."+(i+1)+"/"+idxIdyList.length);
					continue L1;
				}
			} 
				
			String fileNameParsedTxt = "terrain file_"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+".txt";
			if (!fileExistTxt.contains(fileNameParsedTxt)) {
				bilParser(idxIdyList[i], fileNameBil, fileNameParsedTxt); //dat�� �ٽ� �а� txt�� �Ľ��Ѵ�.
				bilParserUTMK(idxIdyList[i], fileNameBil, fileNameParsedTxt); //dat�� �ٽ� �а� txt�� �Ľ��Ѵ�.
			}
			
			
			String fileNameObj = "obj file_"+idxIdyList[i][0]+"_"+idxIdyList[i][1]+".obj";	
			if (!fileExistObj.contains(fileNameObj)) {
				objWriter(idxIdyList[i], fileNameParsedTxt, fileNameObj); 
				objWriterUTMK(idxIdyList[i], fileNameParsedTxt, fileNameObj); 
			}
			
			System.out.println(fileNameParsedTxt+"����Ϸ�....."+(i+1)+"/"+idxIdyList.length);
			
		} //for L1
	}
	

	private static void objWriter(String[] idxidy, String fileNameParsedTxt, String fileNameObj) throws IOException {
		
		FileReader fr = new FileReader(storageDirectory+"DEM txt_Cartesian\\"+fileNameParsedTxt);
		BufferedReader br = new BufferedReader(fr);
		
		ArrayList<Double[]> coordinates = new ArrayList<Double[]>();
		String line;
		
		while ((line=br.readLine())!=null) {
			String[] coorStr = line.split(",");
			Double[] coorDb = new Double[3];
			coorDb[0] = Double.parseDouble(coorStr[0]);
			coorDb[1] = Double.parseDouble(coorStr[1]);
			coorDb[2] = Double.parseDouble(coorStr[2]);			
			coordinates.add(coorDb);
		}
		
		br.close();
		
		FileWriter fw = new FileWriter(targetDirectory+"DEM obj\\"+ fileNameObj);
		BufferedWriter bw = new BufferedWriter(fw);	
		
		//obj ���� ���Ŀ� �°� �����Ѵ�. �ﰢ�� ����� �����.
		bw.write("# Rhino");
		bw.newLine();
		bw.newLine();
		bw.write("g "+idxidy[0]+"_"+idxidy[1]);
		bw.newLine();
		
		for (int i = 0 ; i<coordinates.size() ; i++) {
			bw.write("v "+coordinates.get(i)[0]+" "+coordinates.get(i)[1]+" "+coordinates.get(i)[2]);
			bw.newLine();
		}
		
		for (int i = 0 ; i< 64 ; i++) {
			for (int j=1 ; j<65 ; j++) {
				
				int v = j+(i*65);
			
				bw.write("f ");
				bw.write(v+" "+(v+65)+" "+(v+66));
				bw.newLine();
				bw.write("f ");
				bw.write(v+" "+(v+66)+" "+(v+1));
				bw.newLine();				
				
			}			
		}
		
		bw.close();
		
	}
	
	private static void objWriterUTMK(String[] idxidy, String fileNameParsedTxt, String fileNameObj) throws IOException {
		
		FileReader fr = new FileReader(storageDirectory+"DEM txt_UTMK\\"+fileNameParsedTxt);
		BufferedReader br = new BufferedReader(fr);
		
		ArrayList<Double[]> coordinates = new ArrayList<Double[]>();
		String line;
		
		while ((line=br.readLine())!=null) {
			String[] coorStr = line.split(",");
			Double[] coorDb = new Double[3];
			coorDb[0] = Double.parseDouble(coorStr[0]);
			coorDb[1] = Double.parseDouble(coorStr[1]);
			coorDb[2] = Double.parseDouble(coorStr[2]);			
			coordinates.add(coorDb);
		}
		
		br.close();
		
		FileWriter fw = new FileWriter(targetDirectory+"DEM obj_UTMK\\"+ fileNameObj);
		BufferedWriter bw = new BufferedWriter(fw);	
		
		//obj ���� ���Ŀ� �°� �����Ѵ�. �ﰢ�� ����� �����.
		bw.write("# Rhino");
		bw.newLine();
		bw.newLine();
		bw.write("g "+idxidy[0]+"_"+idxidy[1]);
		bw.newLine();
		
		for (int i = 0 ; i<coordinates.size() ; i++) {
			bw.write("v "+coordinates.get(i)[0]+" "+coordinates.get(i)[1]+" "+coordinates.get(i)[2]);
			bw.newLine();
		}
		
		for (int i = 0 ; i< 64 ; i++) {
			for (int j=1 ; j<65 ; j++) {
				
				int v = j+(i*65);
			
				bw.write("f ");
				bw.write(v+" "+(v+65)+" "+(v+66));
				bw.newLine();
				bw.write("f ");
				bw.write(v+" "+(v+66)+" "+(v+1));
				bw.newLine();	
			}			
		}
		bw.close();
	}

	
	/**
	 * httpRequest�� ������ ���̳ʸ� ������ �޾� �����Ѵ�.
	 * @param address
	 * @param xdofileName
	 */
	private static int sendQueryForBin(String address, String fileName) {
		
		int size = 0;
		
		try {
			
			//�� �κ��� ������ ����ġ���� httpClient�� �ٿ�޾� ��ġ�Ͽ��� �Ѵ�.
			CloseableHttpClient httpClient = HttpClients.createDefault();
			
			HttpGet httpGet = new HttpGet(address);	
			httpGet.addHeader("Referer","http://localhost:4141");
			CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();	
			BufferedInputStream bis = null;
			
			if (entity != null) {
			    long len = entity.getContentLength();
			    bis = new BufferedInputStream(entity.getContent());
			}
			
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(storageDirectory+fileName)));
			int inByte;
			
			while((inByte = bis.read()) != -1) {
				bos.write(inByte);
				size++;
			}
			
			bis.close();
			bos.close();			
			httpResponse.close();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return size;
	}



	/**
	 * xdo ����Ʈ�� �ִ� dat�� �а� txt�� �Ľ��Ͽ� �����Ѵ�.
	 */
	private static void bilParser(String[] idxIdy, String fileName, String fileNameW) throws IOException {
		
		double idx = Double.parseDouble(idxIdy[0]);
		double idy = Double.parseDouble(idxIdy[1]);
		
		double x = unit * (idx - (Math.pow(2, level-1)*10)); //Ÿ���� ���ϴ� x��ǥ(�浵) unit= 0.0010986328125 (�뷫��)
		double y = unit * (idy - (Math.pow(2, level-2)*10));  //Ÿ���� ���ϴ� y��ǥ(����)
		//idx idy���� ���� �� �ִ� ���� ���� 180��, Ȥ�� ���� 90����ŭ
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(storageDirectory+"DEM bil\\"+fileName)));
		
		//�������� ���̷� ����Ѵ�.
		FileWriter fw = new FileWriter(storageDirectory+"DEM txt_latlon\\"+ fileNameW);
		BufferedWriter bw = new BufferedWriter(fw);		
		
		//�ձ� ���� ���� 3���� ��ǥ�� ����Ѵ�.
		FileWriter fwc = new FileWriter(storageDirectory+"DEM txt_Cartesian\\"+ fileNameW);
		BufferedWriter bwc = new BufferedWriter(fwc);	
		
		//terrain height
		//vworld���� �����ϴ� DEM�� 65x65���� ������ �Ǿ� �ִ�.
		for (int yy=64 ; yy>=0 ; yy--) {			
			for (int xx=0 ; xx<65 ; xx++) {				
				double xDegree = x+(unit/64)*xx;
				double yDegree = y+(unit/64)*yy;
				float height = pFloat(bis);
				Vec4 coor = geodeticToCartesian(xDegree, yDegree, height);
				
				//65x65�� ���������� �ļ� �۾��� ����Ͽ� �Ϸķ� ����Ѵ�.
				bwc.write(coor.x+","+coor.y+","+coor.height);
				bwc.newLine();
				bw.write(xDegree+","+(yDegree)+","+height); //�̰��� ���� �浵,����,���̷� ���
				bw.newLine();	
			}		
		}		
		bis.close();
		bw.close();
		bwc.close();
	}
	
	private static void bilParserUTMK(String[] idxIdy, String fileName, String fileNameW) throws IOException {
		
		double idx = Double.parseDouble(idxIdy[0]);
		double idy = Double.parseDouble(idxIdy[1]);
		
		double x = unit * (idx - (Math.pow(2, level-1)*10)); //Ÿ���� ���ϴ� x��ǥ(�浵) unit= 0.0010986328125 (�뷫��)
		double y = unit * (idy - (Math.pow(2, level-2)*10));  //Ÿ���� ���ϴ� y��ǥ(����)
		//idx idy���� ���� �� �ִ� ���� ���� 180��, Ȥ�� ���� 90����ŭ
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(storageDirectory+"DEM bil\\"+fileName)));
		
		FileWriter fwc = new FileWriter(storageDirectory+"DEM txt_UTMK\\"+ fileNameW);
		BufferedWriter bwc = new BufferedWriter(fwc);	
		
		//terrain height
		//vworld���� �����ϴ� DEM�� 65x65���� ������ �Ǿ� �ִ�.
		for (int yy=64 ; yy>=0 ; yy--) {
			
			for (int xx=0 ; xx<65 ; xx++) {
				
				double xDegree = x+(unit/64)*xx;
				double yDegree = y+(unit/64)*yy;
				float height = pFloat(bis);
				GeoPoint xy_ = new GeoPoint(xDegree,yDegree);
				GeoPoint xy = GeoTrans.convert(GeoTrans.GEO, GeoTrans.UTMK, xy_);
				
				//65x65�� ���������� �ļ� �۾��� ����Ͽ� �Ϸķ� ����Ѵ�.
				bwc.write(xy.getX()+","+xy.getY()+","+height);
				bwc.newLine();
			}		
		}
		
		bis.close();
		bwc.close();		
	}

	//World Wind source���� �����ϰ� vworld�� �°� ����
	//https://github.com/nasa/World-Wind-Java/blob/master/WorldWind/src/gov/nasa/worldwind/globes/EllipsoidalGlobe.java
	private static Vec4 geodeticToCartesian(double longitude, double latitude, double metersElevation){
        
        double cosLat = Math.cos(latitude * (Math.PI/180));
        double sinLat = Math.sin(latitude * (Math.PI/180));
        double cosLon = Math.cos(longitude * (Math.PI/180));
        double sinLon = Math.sin(longitude * (Math.PI/180));

        double rpm = // getRadius (in meters) of vertical in prime meridian
        		WGS84_EQUATORIAL_RADIUS / Math.sqrt(1.0 - WGS84_ES * sinLat * sinLat);
     
        //vworld�� ��ǥ������ world wind ����� �ƴ϶� �׳� ��� ��ݰ����� ����� �����
        double x = (WGS84_EQUATORIAL_RADIUS + metersElevation) * cosLat * cosLon;
        double y = (WGS84_EQUATORIAL_RADIUS + metersElevation) * cosLat * sinLon;
        double z = (WGS84_EQUATORIAL_RADIUS + metersElevation) * sinLat;
        
        return new Vec4(x, y, z);
    }
	
	
	private static HashSet<String> getFileNames(String fileLocation, String extension) {
		
		HashSet<String> fileNames = new HashSet<String>(); 		
		File[] files = (new File(fileLocation)).listFiles(); 
		
		// ���丮�� ��� ���� �ʴٸ� 
		if(!(files.length <= 0)){ 			
			for (int i = 0; i < files.length; i++) { 
				if(files[i].isFile() && files[i].getName().endsWith(extension)){ 
				fileNames.add(files[i].getName());
            } 
         } 
       }
		return fileNames;
	}
	
	//4����Ʈ �о float�� ���� 
	private static float pFloat(BufferedInputStream bis) throws IOException {		
		byte[] b = new byte[4];
		int readByteNo = bis.read(b);	
		ArrayUtils.reverse(b);
		return ByteBuffer.wrap(b).getFloat();		
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
	
	
}