package sftpClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class LoadFilesMain {
	static SFTPParameter para=null;
	static String localBasePath="";
	static String remoteBasePath="";
	static String listFile="";
	public static void main(String[] args) {
        try {
        	long start,end;
        	start = System.currentTimeMillis();
        	init();
        	readFileList();
            para.release();
        	System.out.println("文件上传完成");
        	end = System.currentTimeMillis();
        	System.out.println("总耗时"+(end-start));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } 
	}
	
	private static void readFileList() throws Exception {
		String fileName = LoadFilesMain.class.getResource("/").getFile()+"config/"+listFile;
		System.out.println("fileName:"+fileName);
		BufferedReader in = null;
		try{
			in = new BufferedReader(new FileReader(fileName));
			String line = null;
			while ((line = in.readLine()) != null) {
				if(line.startsWith("#")||"".equals(line)|| !line.contains("=")){
					continue;
				}
				uploadFile(line);
			}
		}catch(IOException e){
			throw e;
		}finally{
			if(in != null) in.close();
		}
	}
	
	private static void uploadFile(String line) throws Exception {
		String[]cols = line.split("=");
		if("java".equals(cols[0])){
			uploadJava(cols[1]);
		}else if("biz".equals(cols[0])){
			uploadBiz(cols[1]);
		}else if("mvc".equals(cols[0])){
			uploadMvc(cols[1]);
		}else if("dir".equals(cols[0])){
			uploadDir(new File(localBasePath+cols[1]));
		}else{
			uploadOtherFile(cols[1]);
		}
	}

	private static void uploadDir(File dir) throws Exception {
		if(dir.exists() && dir.isDirectory()){//判断是文件还是目录   
			if(dir.listFiles().length==0){//若目录下没有文件则结束
				 return;  
			}else{
				File delFile[]=dir.listFiles();   
				int i =dir.listFiles().length;   
				for(int j=0;j<i;j++){   
					if(delFile[j].isDirectory()){   
						uploadDir(delFile[j]);//递归调用
					}else{
						String absoluteFileName = delFile[j].getAbsolutePath();
						String relativeFileName = absoluteFileName.replace(localBasePath, "");
						relativeFileName = relativeFileName.replace("\\", "/");
						uploadOtherFile(relativeFileName);
					}
				}   
			}   
		}else{
			throw new Exception("请设置正确的文件夹");
		}
	}

	private static void uploadOtherFile(String fileName) throws InterruptedException, JSchException, SftpException {
		String array[] = fileName.split("/");
		String otherFileName = array[array.length-1];
		String otherFilePath = fileName.substring(0,fileName.lastIndexOf("/"));
		String destLoaclFileName = localBasePath + fileName;
		System.out.println("本地文件:"+destLoaclFileName);
		
		String destRemotePath = remoteBasePath + otherFilePath;
		para.uploadPath = destRemotePath;
		SFTPAccesser.makeDir(para,para.uploadPath);
		System.out.println("开始上传:"+destRemotePath +"/"+ otherFileName);
		SFTPAccesser.uploadFile(para, new File(destLoaclFileName));
	}

	private static void uploadMvc(String fileName) throws InterruptedException, JSchException, SftpException {
		String midPath = "WEB-INF/mvcs/netpay/actions/";
		String array[] = fileName.split("\\.");
		String mvcFileName = array[0]+".xml";
		String destLoaclFileName = localBasePath + midPath + mvcFileName;
		System.out.println("本地文件:"+destLoaclFileName);
		
		String destRemotePath = remoteBasePath + midPath;
		para.uploadPath = destRemotePath;
		SFTPAccesser.makeDir(para,para.uploadPath);
		System.out.println("开始上传:"+destRemotePath + mvcFileName);
		SFTPAccesser.uploadFile(para, new File(destLoaclFileName));
	}

	private static void uploadBiz(String fileName) throws InterruptedException, JSchException, SftpException {
		String midPath = "WEB-INF/bizs/netpay/operations/";
		String array[] = fileName.split("\\.");
		String bizFileName = array[0]+".xml";
		String destLoaclFileName = localBasePath + midPath + bizFileName;
		System.out.println("本地文件:"+destLoaclFileName);
		
		String destRemotePath = remoteBasePath + midPath;
		para.uploadPath = destRemotePath;
		SFTPAccesser.makeDir(para,para.uploadPath);
		System.out.println("开始上传文件:"+destRemotePath +"/"+ bizFileName);
		SFTPAccesser.uploadFile(para, new File(destLoaclFileName));
	}

	private static void uploadJava(String fileName) throws InterruptedException, JSchException, SftpException {
		String midPath = "WEB-INF/classes/";
		String array[] = fileName.split("\\.");
		String classFileName = array[array.length-1]+".class";
		String javaPackageName = fileName.substring(0, fileName.lastIndexOf("."));
		javaPackageName = javaPackageName.replace(".", "/");
		String destLoaclFileName = localBasePath + midPath + javaPackageName + "/" + classFileName;
		System.out.println("本地文件:"+destLoaclFileName);
		String destRemotePath = remoteBasePath + midPath + javaPackageName;
		para.uploadPath = destRemotePath;
		SFTPAccesser.makeDir(para,para.uploadPath);
		System.out.println("开始上传文件:"+destRemotePath +"/"+ classFileName);
		SFTPAccesser.uploadFile(para, new File(destLoaclFileName));
		uploadInnerClass(new File(destLoaclFileName));
	}
	
	private static void uploadInnerClass(File publicClassfile) throws InterruptedException, JSchException, SftpException {
		String dir = publicClassfile.getParent();
		String className = publicClassfile.getName().split("\\.")[0];
		File dirFile = new File(dir);
		if(dirFile.exists()) {
            File[] files = dirFile.listFiles();
            for (File file2 : files) {
                if (file2.isDirectory()) {
                    continue;
                } else {
                	if(file2.getName().startsWith(className+"$")){
                		System.out.println("开始上传内部类文件:"+file2.getPath());
                		System.out.println("开始上传到服务器:"+para.uploadPath);
                		SFTPAccesser.uploadFile(para, new File(file2.getPath()));
                		uploadInnerClass(new File(file2.getPath()));
                	}
                }
            }
		}
	}

	private static void init() {
		para = new SFTPParameter();
		Properties pro = new Properties();
		InputStream in;
		try {
			in = LoadFilesMain.class.getResourceAsStream("/config/conf.properties");
			pro.load(in);
			para.hostName = (String) pro.get("para.hostName");
			para.userName = (String) pro.get("para.userName");
			para.passWord = (String) pro.get("para.passWord");
			para.port = Integer.parseInt((String)pro.get("para.port"));
			remoteBasePath = (String)pro.get("remoteBasePath");
			localBasePath = (String)pro.get("localBasePath");
			listFile = (String)pro.get("listFile");
			System.out.println(para.toString());
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
