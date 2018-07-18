package com.util;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.frame.MyFrame;

public class MyOpencv {

	String bodyImg;	//小人图片，用于匹配
	String sourceImg;	//每一次起跳前的截图
	String whiteDotImg;	//小白点，能匹配到就是终点
	Point bodyPoint; 	//小人的左上角坐标
	Point startPoint;	//起点，即小人的中心
	Point endPoint;		//终点，下一个落点的中心
	long distance;	//起点到终点的距离
	int presstime;		//按压的时间，
	
	private Mat body;
	private Mat source;
	private Mat result;
	private Mat imgCanny; //边缘检测像素图，灰度图
	private MyFrame frame;
	
	public MyOpencv(String bodyImg,String sourceImg,String whiteDotImg,MyFrame frame) {
		
		this.bodyImg = bodyImg;
		this.sourceImg = sourceImg;
		this.whiteDotImg = whiteDotImg;
		this.startPoint = new Point(0,0);
		this.endPoint = new Point(0,0);
		this.bodyPoint = new Point(0,0);
		this.distance = 0;
		this.presstime = 0;
			
		this.frame = frame;
	}
	
	//采用图像模板匹配，模板为小人，匹配小人在截图中的位置，从而得到起点坐标
	public void matchBody() {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);  	
		//像素矩阵初始化
		this.body = Imgcodecs.imread(this.bodyImg);
		this.source = Imgcodecs.imread(this.sourceImg);
		this.result = Mat.zeros(source.rows(), source.cols(),source.type());//初始化全为0
    	Imgproc.matchTemplate(source, body, result, Imgproc.TM_CCOEFF_NORMED);
    	
    	//在给定的矩阵中寻找最大和最小值(包括它们的位置),minMaxLocResult 返回的 maxLoc为匹配的模板在截图中的左上位置
    	MinMaxLocResult minMaxLocResult = Core.minMaxLoc(result);
    	//bodyPoint表示小人在截图中的左上角坐标
    	this.bodyPoint = new Point(minMaxLocResult.maxLoc.x, minMaxLocResult.maxLoc.y);
    	System.out.println("小人坐标起点："+this.bodyPoint);
    	
    	//根据小人的高度和宽度以及在截图中的位置，粗略得到小人的中心点，即起点
    	Point point = 
    			new Point(this.bodyPoint.x + body.width()/2, this.bodyPoint.y + 0.8*body.height());
    	this.setStartPoint(point);
    	System.out.println("起点坐标："+this.startPoint);
    	
    	//复制一份截图，添加小人的标记和起点标记
    	Mat mark = this.source.clone();	
    	//绘制一个矩形把小人框起来
    	Imgproc.rectangle(mark, bodyPoint, 
    			new Point((int)(this.bodyPoint.x+this.body.cols()), (int)(this.bodyPoint.y+this.body.rows())),new Scalar(0,255,0),5); 
    	//绘制一个点，表示标出来的起点
    	Imgproc.circle(mark,startPoint , 
    			10,new Scalar(255,0,0), -1);
    	//将标记好的图写出，生成mark.jpg
    	Imgcodecs.imwrite("images/mark.jpg", mark);   	
	}

	//边缘检测
	public void edgeDetection() {
		
		//初始化矩阵为0，宽高和原截图一样
		Mat imgBlur = Mat.zeros(source.rows(), source.cols(),CvType.CV_32FC1);
		//对图片进行高斯模糊处理，去除噪点
    	Imgproc.GaussianBlur(source, imgBlur, new Size(5,5), 0);
    	//将经过高斯模糊处理的图片写出到文件夹
    	Imgcodecs.imwrite("images/imgBlur.jpg", imgBlur);
    	//初始化矩阵为0，宽高和原截图一样
    	this.imgCanny = Mat.zeros(this.source.rows(), this.source.cols(),CvType.CV_32FC1);
    	//高斯模糊处理后的图片进行边缘检测处理
    	Imgproc.Canny(imgBlur, this.imgCanny, 1, 10);
    	
    	//先把图片中的小人抹掉，以免裁剪后出现小人影响计算
    	for(int i = (int)this.bodyPoint.x; i <= (int)(this.bodyPoint.x+this.body.width()); i++) {
    		for(int j = (int)this.bodyPoint.y; j <= (int)(this.bodyPoint.y+this.body.height()); j++) {
    			this.imgCanny.put(j, i, 0);//注意这里的i和j，x坐标在像素图中是列数，y是行数
    		}
    	}
    	//边缘检测后的图片输出到文件夹
    	Imgcodecs.imwrite("images/imgCanny.jpg", this.imgCanny);
    	  	
	}
	
	public void findEndPoint() {
		
		//先对边缘检测后的图片进行剪切
		//剪切是因为落地点只会在图片的上半部分，
		//而且是在分数的下边，所以可以划分出一个大概的区域(从坐标（0，图片高度的1/4处）开始的宽为原图片的宽，长度为原图片的1/4的矩形)
		Mat imgCut = new Mat(this.imgCanny, 
				new Rect(0, (int)Math.round(source.height()*0.25), source.width(), (int)Math.round(source.height()*0.25)));
		//复制一份，输出到文件夹
    	Mat imgCopy = imgCut.clone();
    	Imgcodecs.imwrite("images/imgCut.jpg", imgCopy);
    	
    	imgCopy.convertTo(imgCopy, CvType.CV_64FC3);	//不做这一步下面的计算会报错
    	int size = (int) (imgCopy.total() * imgCopy.channels());	//像素点总数*通道数，灰度图通道数为1
    	System.out.println("裁剪后的图片宽高："+imgCopy.rows()+","+imgCopy.cols());
    	System.out.println("像素点数量："+size);
    	System.out.println("通道数："+imgCopy.channels());
    	double data[] = new double[size];	//存储像素点的一维数组
    	imgCopy.get(0, 0, data);	//将从(0,0)像素点的值存放到数组中,只有两种值，0为黑色，255为白色
    	
    	//不出错的话裁剪后的图基本只剩终点所在的方块，从上至下，从左至右
    	//找到第一个白色点A(x1,y1),以及横坐标最大的白色点B(x2,y2),终点的坐标即为(x1,y2)
    	Point point = new Point(0,0);
    	int maxX = 0; //存储最大x坐标
    	for(int i = 0; i < imgCopy.rows();i++) {
    		for(int j = 0; j < imgCopy.cols(); j++) {
    			//坐标在一维数组的下标转换(i,j)→[i*每行的列数+当前所在的列数j]
    			if((int)data[i*imgCopy.cols()+j] == 255&&point.x == 0) {
    				point.x = j;
    			}
    			if((int)data[i*imgCopy.cols()+j] == 255 && j>maxX && point.x!=0) {
    				point.y = i;
    				maxX = j;
    			}
    		}
    	}
    	//标记在裁剪后的图片中的终点，这时得到的坐标并不是真正的终点坐标，因为是以裁剪后的图片左上角为起点
    	Imgproc.circle(imgCopy, endPoint,10,new Scalar(255,0,255), -1);
    	Imgcodecs.imwrite("images/imgCut.jpg", imgCopy);
    	point.y += this.source.height()*0.25; //y坐标应加上裁剪的1/4,横坐标不变
    	this.setEndPoint(point);
    	Imgproc.circle(this.imgCanny, this.endPoint,10,new Scalar(255,0,255), -1);   	
    	Imgcodecs.imwrite("images/imgCanny.jpg", imgCanny);
	}

	public void setDistance() {
		
		this.distance = 
				Math.round(Math.sqrt(Math.pow(this.startPoint.x-this.endPoint.x, 2)
						+Math.pow(this.startPoint.y-this.endPoint.y, 2)));
	}
	
	public boolean findWhiteDot() {
		
		Mat whiteDot = Imgcodecs.imread(this.whiteDotImg);
    	source = Imgcodecs.imread(this.sourceImg);
    	Mat dotResult = Mat.zeros(this.source.rows(), this.source.cols(),this.source.type());
    	Imgproc.matchTemplate(this.source, whiteDot, dotResult, Imgproc.TM_CCOEFF_NORMED);
    	MinMaxLocResult dotLocResult = Core.minMaxLoc(dotResult);
    	
    	if(dotLocResult.maxVal > 0.8) {
    		System.out.println("小白点匹配值："+dotLocResult.maxVal);
    		System.out.println("找到小白点");
    		this.endPoint.x = dotLocResult.maxLoc.x + whiteDot.width()/2;
    		this.endPoint.y = dotLocResult.maxLoc.y + whiteDot.height()/2;
    		
    		Mat imgResult = source.clone();
    		Imgproc.circle(imgResult, endPoint, 10, new Scalar(255, 255, 0),-1);
    		Imgcodecs.imwrite("images/dotLocResult.jpg", imgResult);
    		
    		return true;
    	}
    	return false;
	}
	public void setPretime(double x) {
		this.presstime = (int)(this.distance*x);
	}
	public Point getStartPoint() {
		return startPoint;
	}

	public void setStartPoint(Point startPoint) {
		this.startPoint = startPoint;
	}

	public Point getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(Point endPoint) {
		this.endPoint = endPoint;
	}

	public int getPresstime() {
		return presstime;
	}

	public void go() {
		this.matchBody();
		this.edgeDetection();
		if(!findWhiteDot()) {
			this.findEndPoint();
		}
		this.setDistance();
		this.setPretime(Double.parseDouble(this.frame.getTextField().getText()));
	}
	
}
