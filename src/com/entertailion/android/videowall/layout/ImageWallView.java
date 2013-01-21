/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.entertailion.android.videowall.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * A view which displays a grid of images.
 */
public class ImageWallView extends ViewGroup {

	private static final String LOG_TAG = "ImageWallView";

	private final Context context;
	private final Random random;

	private final int imageHeight;
	private final int imageWidth;
	private final int interImagePadding;

	private ImageView[] images;
	private List<Integer> unInitializedImages;

	private int numberOfColumns;
	private int numberOfRows;

	private int displayWidth, displayHeight;

	public ImageWallView(Context context, int imageWidth, int imageHeight, int interImagePadding) {
		super(context);
		this.context = context;
		random = new Random();

		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.interImagePadding = interImagePadding;
		this.images = new ImageView[0];
		this.unInitializedImages = new ArrayList<Integer>();

		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		displayWidth = size.x;
		displayHeight = size.y;
		Log.d(LOG_TAG, "width=" + displayWidth);
		Log.d(LOG_TAG, "height=" + displayHeight);
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {

		// create enough columns to fill view's width, plus an extra column at
		// either side to allow
		// images to have diagonal offset across the screen.
		numberOfColumns = width / (imageWidth + interImagePadding) + 2;
		// create enough rows to fill the view's height (adding an extra row at
		// bottom if necessary).
		numberOfRows = height / (imageHeight + interImagePadding);
		numberOfRows += (height % (imageHeight + interImagePadding) == 0) ? 0 : 1;

		if ((numberOfRows <= 0) || (numberOfColumns <= 0)) {
			throw new IllegalStateException("Error creating an ImageWallView with " + numberOfRows + " rows and " + numberOfColumns
					+ " columns. Both values must be greater than zero.");
		}

		if (images.length < (numberOfColumns * numberOfRows)) {
			images = Arrays.copyOf(images, numberOfColumns * numberOfRows);
		}

		removeAllViews();
		for (int col = 0; col < numberOfColumns; col++) {
			for (int row = 0; row < numberOfRows; row++) {
				int elementIdx = getElementIdx(col, row);
				if (images[elementIdx] == null) {
					ImageView thumbnail = new ImageView(context);
					thumbnail.setLayoutParams(new LayoutParams(imageWidth, imageHeight));
					images[elementIdx] = thumbnail;
					unInitializedImages.add(elementIdx);
				}
				addView(images[elementIdx]);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		int width = getDefaultSize(displayMetrics.widthPixels, widthMeasureSpec);
		int height = getDefaultSize(displayMetrics.heightPixels, heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		for (int col = 0; col < numberOfColumns; col++) {
			for (int row = 0; row < numberOfRows; row++) {
				int x = (col - 1) * (imageWidth + interImagePadding) + (row * (imageWidth / numberOfRows));
				int y = row * (imageHeight + interImagePadding);
				ImageView imageView = images[col * numberOfRows + row];
				imageView.layout(x, y, x + imageWidth, y + imageHeight);
				imageView.setTag(isTotallyVisible(x, y, x + imageWidth, y + imageHeight));
			}
		}
	}

	public int getXPosition(int col, int row) {
		return images[getElementIdx(col, row)].getLeft();
	}

	public int getYPosition(int col, int row) {
		return images[getElementIdx(col, row)].getTop();
	}

	private int getElementIdx(int col, int row) {
		return (col * numberOfRows) + row;
	}

	public ImageView hideImage(int col, int row) {
		ImageView imageView = images[getElementIdx(col, row)];
		imageView.setVisibility(View.INVISIBLE);
		return imageView;
	}

	public void showImage(int col, int row) {
		images[getElementIdx(col, row)].setVisibility(View.VISIBLE);
	}

	public void setImageDrawable(int col, int row, Drawable drawable) {
		int elementIdx = getElementIdx(col, row);
		// manually boxing elementIdx to avoid calling List.remove(int position)
		// method overload
		unInitializedImages.remove(new Integer(elementIdx));
		images[elementIdx].setImageDrawable(drawable);
	}

	public Drawable getImageDrawable(int col, int row) {
		int elementIdx = getElementIdx(col, row);
		return images[elementIdx].getDrawable();
	}

	public Pair<Integer, Integer> getNextLoadTarget(boolean isVideo) {
		int nextElement;
		boolean totallyVisible = true;
		do {
			if (unInitializedImages.isEmpty()) {
				nextElement = random.nextInt(images.length);
			} else {
				nextElement = unInitializedImages.get(random.nextInt(unInitializedImages.size()));
			}
			if (isVideo) {
				totallyVisible = (Boolean) images[nextElement].getTag();
			}
		} while (images[nextElement].getVisibility() != View.VISIBLE || !totallyVisible);

		int col = nextElement / numberOfRows;
		int row = nextElement % numberOfRows;
		return new Pair<Integer, Integer>(col, row);
	}

	public boolean allImagesLoaded() {
		return unInitializedImages.isEmpty();
	}

	private boolean isTotallyVisible(int x1, int y1, int x2, int y2) {
		if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0) {
			return false;
		}
		if (x1 > displayWidth || x2 > displayWidth) {
			return false;
		}
		if (y1 > displayHeight || y2 > displayHeight) {
			return false;
		}
		return true;
	}

}
