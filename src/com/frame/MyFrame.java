package com.frame;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Point;

import com.util.MyOpencv;

public class MyFrame extends JFrame{
	
	private Label label;
	private TextField textField;
	private Button startButton;
	private Button stopButton;
	private JPanel panel;
	private TextArea textArea;
	
	//截图和上传图片的adb命令
	private String cmdScreen = new String("adb shell screencap /sdcard/1.png");
	private String cmdPull = new String("adb pull /sdcard/1.png D:\\opencv\\WeixinJump\\images");
	
	private Point p1,p2;
	MyOpencv myOpencv = new MyOpencv("images/body.jpg", "images/1.png","images/whiteDot.jpg",this);
	private Timer timer;
	
	public MyFrame() {
		
		this.setTitle("微信跳一跳");
		this.setSize(300,300);
		
		this.setLocationRelativeTo(null);
		
		panel = new JPanel(new GridLayout(2, 2));
		this.add(panel,BorderLayout.CENTER);
		
		this.timer = new Timer();
		this.label = new Label("弹跳系数：");
		this.textField = new TextField("1.35");
		this.textArea = new TextArea("分辨率1080*1920弹跳系数：1.35");
		this.startButton = new Button("开始");
		this.stopButton = new Button("停止");
		this.panel.add(label);
		this.panel.add(textField);
		this.panel.add(startButton);
		this.panel.add(stopButton);
		this.add(textArea,BorderLayout.SOUTH);
		
		this.setVisible(true);
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub
				System.exit(0);
			}
			
		});
		
		this.startButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				timer = new Timer();
				//设置了一个定时器，后面参数1000和5000的意思启动时延迟1秒执行，每5秒执行一次，就能实现一直跳了
				timer.schedule(new TimerTask() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						execAdbCmd(cmdScreen); //截取当前手机屏幕并保存在手机中
						execAdbCmd(cmdPull); //将手机中的截图上传到电脑的文件夹中
						
						//调用opencv封装好的方法
						myOpencv.go();
						//这里获取起点的坐标，在起点坐标附近再随机产生一个坐标，保证每次触摸坐标都不一样，不过这样做还是会被判做操作异常
						p1 = myOpencv.getStartPoint();
						p2 = new Point(p1.x+Math.random()*10,p1.y+Math.random()*15);
								
						System.out.println("触摸坐标为"+p1+" "+p2);
						//按压屏幕的adb指令
						String cmd = 
								String.format("adb shell input swipe %d %d %d %d %d ",
										(int)p1.x,(int)p1.y,(int)p2.x,(int)p2.y,myOpencv.getPresstime());
						execAdbCmd(cmd);
						System.out.println("ok");
					}
				}, 1000,5000);
			}
			
		});
		
		this.stopButton.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					timer.cancel();
					System.out.println("定时器取消");
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				
			}
		});
	}
	
	public void execAdbCmd(String cmd) {
    	try {
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
			System.out.println("execute...");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public TextField getTextField() {
		return textField;
	}

	public void setTextField(TextField textField) {
		this.textField = textField;
	}
	
}
