/*
 * Created on Mar 7, 2013
 * Created by Paul Gardner
 * 
 * Copyright 2013 Azureus Software, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package com.aelitis.azureus.plugins.view3d;

import java.nio.IntBuffer;

import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Cylinder;
import org.lwjgl.util.glu.Disk;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;

public class ViewTest2 {
	static void drawCylinder(float R, float H, int completion, int color) {
		float height = H * completion / 100;

		Disk disk = new Disk();
		disk.draw(0.0f, R, 16, 1);
		Cylinder cylinder = new Cylinder();
		cylinder.draw( R, R, height, 16, 1);
		GL11.glTranslatef(0, 0, height);
		disk = new Disk();
		disk.draw(0.0f, R, 16, 1);


		cylinder = new Cylinder();
		cylinder.draw( R, R, H - height, 16, 1);

		GL11.glTranslatef(0, 0, H - height);


	}

	static void drawTorus(float r, float R, int nsides, int rings) {
		double ringDelta = 2.0f * (double) Math.PI / rings;
		double sideDelta = 2.0f * (double) Math.PI / nsides;
		double theta = 0.0f, cosTheta = 1.0f, sinTheta = 0.0f;
		for (int i = rings - 1; i >= 0; i--) {
			double theta1 = theta + ringDelta;
			double cosTheta1 = (double) Math.cos(theta1);
			double sinTheta1 = (double) Math.sin(theta1);
			GL11.glBegin(GL11.GL_QUAD_STRIP);
			float phi = 0.0f;
			for (int j = nsides; j >= 0; j--) {
				phi += sideDelta;
				double cosPhi = (double) Math.cos(phi);
				double sinPhi = (double) Math.sin(phi);
				double dist = R + r * cosPhi;
				GL11.glNormal3d(cosTheta1 * cosPhi, -sinTheta1 * cosPhi, sinPhi);
				GL11.glVertex3d(cosTheta1 * dist, -sinTheta1 * dist, r * sinPhi);
				GL11.glNormal3d(cosTheta * cosPhi, -sinTheta * cosPhi, sinPhi);
				GL11.glVertex3d(cosTheta * dist, -sinTheta * dist, r * sinPhi);
			}
			GL11.glEnd();
			theta = theta1;
			cosTheta = cosTheta1;
			sinTheta = sinTheta1;
		}
	}

	
	static int[]
	getViewport()
	{
		IntBuffer viewport = BufferUtils.createIntBuffer(16);
		   
		GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
		
		int[] result = new int[4];
		
		viewport.position( 0 );
		
		viewport.get( result );
		
		return( result );
	}
	
	
	static double[][] jits = { {0.5625, 0.4375}, {0.0625, 0.9375}, {0.3125, 0.6875}, {0.6875, 0.8125}, {0.8125, 0.1875}, {0.9375, 0.5625}, {0.4375, 0.0625}, {0.1875, 0.3125}};
	
	static void accFrustum(double left, double right, double bottom,
			double top, double near, double far, double pixdx, 
			double pixdy, double eyedx, double eyedy, 
			double focus)
	{
		double xwsize, ywsize; 
		double dx, dy;
		int[] viewport = getViewport();

		xwsize = right - left;
		ywsize = top - bottom;
		dx = -(pixdx*xwsize/(double) viewport[2] + 
				eyedx*near/focus);
		dy = -(pixdy*ywsize/(double) viewport[3] + 
				eyedy*near/focus);

		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glFrustum (left + dx, right + dx, bottom + dy, top + dy, 
				near, far);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glTranslated (-eyedx, -eyedy, 0.0);
	}

	static void accPerspective(double fovy, double aspect, 
			double near, double far, double pixdx, double pixdy, 
			double eyedx, double eyedy, double focus)
	{
		double fov2,left,right,bottom,top;
		fov2 = ((fovy*Math.PI) / 180.0) / 2.0;

		top = near / (Math.cos(fov2) / Math.sin(fov2));
		bottom = -top;
		right = top * aspect;
		left = -right;

		accFrustum (left, right, bottom, top, near, far,
				pixdx, pixdy, eyedx, eyedy, focus);
	}
	
	public static void main(String [] args) {
		final Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Composite comp = new Composite(shell, SWT.NONE);
		comp.setLayout(new FillLayout());
		GLData data = new GLData ();
		data.doubleBuffer = true;
		data.accumAlphaSize = 8;
		data.accumBlueSize = 8;
		data.accumGreenSize = 8;
		data.accumRedSize = 8;
		
		final GLCanvas canvas = new GLCanvas(comp, SWT.NONE, data);

		canvas.setCurrent();
		try {
			GLContext.useContext(canvas);
		} catch(LWJGLException e) { e.printStackTrace(); }

		canvas.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event event) {
				Rectangle bounds = canvas.getBounds();
				float fAspect = (float) bounds.width / (float) bounds.height;
				canvas.setCurrent();
				try {
					GLContext.useContext(canvas);
				} catch(LWJGLException e) { e.printStackTrace(); }
				GL11.glViewport(0, 0, bounds.width, bounds.height);
				GL11.glMatrixMode(GL11.GL_PROJECTION);
				GL11.glLoadIdentity();
				GLU.gluPerspective(45.0f, fAspect, 0.5f, 400.0f);
				GL11.glMatrixMode(GL11.GL_MODELVIEW);
				GL11.glLoadIdentity();
			}
		});

		
		GL11.glLineWidth(1);
		GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		
		GL11.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GL11.glColor3f(1.0f, 0.0f, 0.0f);
		GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
		GL11.glClearDepth(1.0);
		GL11.glLineWidth(2);
		GL11.glEnable(GL11.GL_DEPTH_TEST);

		
		GL11.glShadeModel (GL11.GL_FLAT);

		GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GL11.glClearAccum(0.0f, 0.0f, 0.0f, 0.0f);
		
		shell.setText("SWT/LWJGL Example");
		shell.setSize(640, 480);
		shell.open();
		
		final Runnable run = new Runnable() {
			float rot = 0;
			@Override
			public void run() {
				if (!canvas.isDisposed()) {
					canvas.setCurrent();
					try {
						GLContext.useContext(canvas);
					} catch(LWJGLException e) { e.printStackTrace(); }
					
						int ACSIZE = 8;
						
						int[] viewport = getViewport();

						GL11.glClear(GL11.GL_ACCUM_BUFFER_BIT);
						
						for (int jitter = 0; jitter < ACSIZE; jitter++) {
							GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
					      
							double jit_x = jits[jitter][0];
							double jit_y = jits[jitter][1];
						
							accPerspective (50.0, (double) viewport[2]/(double) viewport[3], 
									1.0, 15.0, jit_x, jit_y, 0.0, 0.0, 1.0);
					    
					
							{
							
								GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
								GL11.glClearColor(.3f, .5f, .8f, 1.0f);
								GL11.glLoadIdentity();
								GL11.glTranslatef(0.0f, 0.0f, -10.0f);
								float frot = rot;
								GL11.glRotatef(0.15f * rot, 2.0f * frot, 10.0f * frot, 1.0f);
								GL11.glRotatef(0.3f * rot, 3.0f * frot, 1.0f * frot, 1.0f);
								GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
								GL11.glColor3f(0.9f, 0.9f, 0.9f);
								drawCylinder( 2, 3, 50, 0 );
								drawTorus(1, 1.9f + ((float) Math.sin((0.004f * frot))), 15, 15);
							}
					
					   
					   
							GL11.glAccum(GL11.GL_ACCUM, 1.0f/ACSIZE);
						}
						
					   GL11.glAccum( GL11.GL_RETURN, 1.0f);
					   GL11.glFlush();
				
					
					   rot+=0.1;

					canvas.swapBuffers();
					display.asyncExec(this);
				}
			}
		};
		canvas.addListener(SWT.Paint, new Listener() {
			@Override
			public void handleEvent(Event event) {
				run.run();
			}
		});
		display.asyncExec(run);

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}