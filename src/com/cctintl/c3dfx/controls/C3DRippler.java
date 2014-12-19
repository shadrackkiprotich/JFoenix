package com.cctintl.c3dfx.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import com.cctintl.c3dfx.converters.MaskTypeConverter;
import com.sun.javafx.css.converters.PaintConverter;

@DefaultProperty(value="control")
public class C3DRippler extends StackPane {

	public static enum RipplerPos{FRONT, BACK};
	public static enum RipplerMask{CIRCLE, RECT};

	protected RippleGenerator rippler;
	protected Pane ripplerPane;
	protected Node control;
	private double defaultRadius = 200;
	private double minRadius = 100;
	private double rippleRadius = 150;
	private boolean enabled = true;
	private boolean toggled = false;

	public C3DRippler(){
		this(null,RipplerMask.RECT,RipplerPos.FRONT);
	}

	public C3DRippler(Node control){
		this(control, RipplerMask.RECT, RipplerPos.FRONT);
	}

	public C3DRippler(Node control, RipplerPos pos){
		this(control, RipplerMask.RECT , pos);
	}

	public C3DRippler(Node control, RipplerMask mask){
		this(control, mask , RipplerPos.FRONT);
	}

	public C3DRippler(Node control, RipplerMask mask,  RipplerPos pos){
		super();		
		initialize();
		this.maskType.set(mask);
		this.position.set(pos);
		setControl(control);
	}	

	/***************************************************************************
	 *                                                                         *
	 * Setters / Getters                                                       *
	 *                                                                         *
	 **************************************************************************/

	public void setControl(Node control){
		if(control!=null){
			this.control = control;

			//			if(this.control instanceof Control){
			//				((Control)this.control).widthProperty().addListener((o,oldVal,newVal) -> {ripplerPane.setMaxWidth((double) newVal);});
			//				((Control)this.control).heightProperty().addListener((o,oldVal,newVal) -> {ripplerPane.setMaxHeight((double) newVal);});
			//			}else if(this.control instanceof Pane){
			//				((Pane)this.control).widthProperty().addListener((o,oldVal,newVal) -> {ripplerPane.setMaxWidth((double) newVal);});
			//				((Pane)this.control).heightProperty().addListener((o,oldVal,newVal) -> {ripplerPane.setMaxHeight((double) newVal);});
			//			}

			// create rippler panels
			rippler = new RippleGenerator();
			ripplerPane = new StackPane();
			ripplerPane.getChildren().add(rippler);

			// set the control postion and listen if it's changed
			if(this.position.get() == RipplerPos.BACK) ripplerPane.getChildren().add(this.control);
			else this.getChildren().add(this.control);

			this.position.addListener((o,oldVal,newVal)->{
				if(this.position.get() == RipplerPos.BACK) ripplerPane.getChildren().add(this.control);
				else this.getChildren().add(this.control);	
			});			

			control.boundsInParentProperty().addListener((o,oldVal,newVal)->{
				rippleRadius = newVal.getWidth();
				if(rippleRadius > defaultRadius)
					rippleRadius = defaultRadius;
				if(rippleRadius < minRadius)
					rippleRadius = minRadius;
			});
			
			this.getChildren().add(ripplerPane);

			// add listeners
			initListeners();
			this.requestLayout();
		}
	}

	public Node getControl(){
		return this.control;
	}

	public void setPostion(RipplerPos pos){
		this.position.set(pos);
	}

	public RipplerPos getPostion(){
		return this.position.get();
	}

	public void setEnabled(boolean enable){
		this.enabled = enable;
	}

	// methods that can be changed by extending the rippler class
	/**
	 *  clipping mask
	 * @return
	 */
	protected Shape getMask(){
		Shape mask = new Rectangle(control.getBoundsInParent().getWidth() - 0.1,control.getBoundsInParent().getHeight() - 0.1); // -0.1 to prevent resizing the anchor pane
		if(maskType.get().equals(C3DRippler.RipplerMask.CIRCLE))
			mask = new Circle(control.getBoundsInParent().getWidth()/2 , control.getBoundsInParent().getHeight()/2, (control.getBoundsInParent().getWidth()/2) - 0.1, Color.BLUE);	
		return mask;
	}
	/**
	 *  mouse listeners
	 * @return
	 */
	protected void initListeners(){
		ripplerPane.setOnMousePressed((event) -> {
			createRipple(event.getX(),event.getY());
			if(this.position.get() == RipplerPos.FRONT)
				this.control.fireEvent(event);
		});
		ripplerPane.setOnMouseReleased((event) -> {
			if(this.position.get() == RipplerPos.FRONT)
				this.control.fireEvent(event);
		});
		ripplerPane.setOnMouseClicked((event) -> {
			if(this.position.get() == RipplerPos.FRONT )
				this.control.fireEvent(event);
		});
	}
	/**
	 *  create Ripple effect
	 * @return
	 */
	protected void createRipple(double x, double y){
		rippler.setGeneratorCenterX(x);
		rippler.setGeneratorCenterY(y);
		rippler.createRipple();
	}

	public void toggle(){
		if(!toggled){
			rippler.overlayRect.animation.setRate(1);
			rippler.overlayRect.animation.play();
		}else{
			rippler.overlayRect.animation.setRate(-1);
			rippler.overlayRect.animation.play();
		}
		toggled = !toggled;
	}

	/**
	 * Generates ripples on the screen every 0.3 seconds or whenever
	 * the createRipple method is called. Ripples grow and fade out
	 * over 0.6 seconds
	 */
	class RippleGenerator extends Group {

		private double generatorCenterX = 0;
		private double generatorCenterY = 0;
		private OverLayRipple overlayRect;
		private boolean generating = false;

		public void createRipple() {
			if(enabled){
				if(!generating){
					generating = true;
					// create overlay once then change its color later 
					if(overlayRect == null){
						overlayRect = new OverLayRipple();
						overlayRect.setClip(getMask());
						getChildren().add(overlayRect);
					}
					overlayRect.setFill(new Color(((Color)ripplerFill.get()).getRed(), ((Color)ripplerFill.get()).getGreen(), ((Color)ripplerFill.get()).getBlue(),0.2));

					// create the ripple effect
					final Ripple ripple = new Ripple(generatorCenterX, generatorCenterY);				
					ripple.setClip(getMask());
					getChildren().add(ripple);			

					if(overlayRect.getOpacity()!=1){
						overlayRect.animation.setRate(1);
						overlayRect.animation.play();
					}
					ripple.inAnimation.play();

					// create fade out transition for the ripple
					ripplerPane.setOnMouseReleased((e)->{
						generating = false;
						overlayRect.animation.setRate(-1);						
						ripple.inAnimation.pause();
						double fadeOutRadious = rippleRadius + 20;
						if(ripple.radiusProperty().get() < rippleRadius*0.5)
							fadeOutRadious = rippleRadius;

						Timeline outAnimation = new Timeline(
								new KeyFrame(Duration.seconds(0.3),
										new KeyValue(ripple.radiusProperty(), fadeOutRadious ,Interpolator.LINEAR),
										new KeyValue(ripple.opacityProperty(), 0, Interpolator.EASE_BOTH)
										));
						outAnimation.play();
						overlayRect.animation.play();
						outAnimation.setOnFinished((event)->{
							getChildren().remove(ripple);	
						});
					});
				}
			}
		}

		public void setGeneratorCenterX(double generatorCenterX) {
			this.generatorCenterX = generatorCenterX;
		}

		public void setGeneratorCenterY(double generatorCenterY) {
			this.generatorCenterY = generatorCenterY;
		}

		private class OverLayRipple extends Rectangle{
			Timeline animation = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(opacityProperty(),  0,Interpolator.EASE_BOTH)
							),new KeyFrame(Duration.seconds(0.2),
									new KeyValue(opacityProperty(), 1,Interpolator.EASE_BOTH)
									));
			public OverLayRipple() {
				super(control.getBoundsInParent().getWidth() - 0.1,control.getBoundsInParent().getHeight() - 0.1);
			}
		}

		private class Ripple extends Circle {

			Timeline inAnimation = new Timeline(
					new KeyFrame(Duration.ZERO,
							new KeyValue(radiusProperty(),  0,Interpolator.LINEAR),
							new KeyValue(opacityProperty(), 1,Interpolator.EASE_BOTH)
							),new KeyFrame(Duration.seconds(0.3), 
									new KeyValue(radiusProperty(),  rippleRadius ,Interpolator.LINEAR)					
									));

			private Ripple(double centerX, double centerY) {
				super(centerX, centerY, 0, null);	
				if(ripplerFill.get() instanceof Color){
					Color circleColor = new Color(((Color)ripplerFill.get()).getRed(), ((Color)ripplerFill.get()).getGreen(), ((Color)ripplerFill.get()).getBlue(),0.3);
					setStroke(circleColor);
					setFill(circleColor);
				}else{
					setStroke(ripplerFill.get());
					setFill(ripplerFill.get());
				}
			}
		}
	}

	/***************************************************************************
	 *                                                                         *
	 * Stylesheet Handling                                                     *
	 *                                                                         *
	 **************************************************************************/


	private static final String DEFAULT_STYLE_CLASS = "c3d-rippler";

	private void initialize() {
		this.getStyleClass().add(DEFAULT_STYLE_CLASS);        
	}

	private StyleableObjectProperty<Paint> ripplerFill = new SimpleStyleableObjectProperty<Paint>(StyleableProperties.RIPPLER_FILL, C3DRippler.this, "ripplerFill", Color.rgb(0, 200, 255));

	public Paint getRipplerFill(){
		return ripplerFill == null ? Color.rgb(0, 200, 255) : ripplerFill.get();
	}
	public StyleableObjectProperty<Paint> ripplerFillProperty(){		
		return this.ripplerFill;
	}
	public void setRipplerFill(Paint color){
		this.ripplerFill.set(color);
	}

	private StyleableObjectProperty<RipplerMask> maskType = new SimpleStyleableObjectProperty<RipplerMask>(StyleableProperties.MASK_TYPE, C3DRippler.this, "maskType", RipplerMask.RECT );

	public RipplerMask getMaskType(){
		return maskType == null ? RipplerMask.RECT : maskType.get();
	}
	public StyleableObjectProperty<RipplerMask> maskTypeProperty(){		
		return this.maskType;
	}
	public void setMaskType(RipplerMask type){
		this.maskType.set(type);
	}

	protected ObjectProperty<RipplerPos> position = new SimpleObjectProperty<RipplerPos>();

	public RipplerPos getPosition(){
		return position == null ? RipplerPos.FRONT : position.get();
	}
	public ObjectProperty<RipplerPos> positionProperty(){		
		return this.position;
	}


	private static class StyleableProperties {
		private static final CssMetaData< C3DRippler, Paint> RIPPLER_FILL =
				new CssMetaData< C3DRippler, Paint>("-fx-rippler-fill",
						PaintConverter.getInstance(), Color.rgb(0, 200, 255)) {
			@Override
			public boolean isSettable(C3DRippler control) {
				return control.ripplerFill == null || !control.ripplerFill.isBound();
			}
			@Override
			public StyleableProperty<Paint> getStyleableProperty(C3DRippler control) {
				return control.ripplerFillProperty();
			}
		};
		private static final CssMetaData< C3DRippler, RipplerMask> MASK_TYPE =
				new CssMetaData< C3DRippler, RipplerMask>("-fx-mask-type", MaskTypeConverter.getInstance(), RipplerMask.RECT) {
			@Override
			public boolean isSettable(C3DRippler control) {
				return control.maskType == null || !control.maskType.isBound();
			}
			@Override
			public StyleableProperty<RipplerMask> getStyleableProperty(C3DRippler control) {
				return control.maskTypeProperty();
			}
		};

		private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
		static {
			final List<CssMetaData<? extends Styleable, ?>> styleables =
					new ArrayList<CssMetaData<? extends Styleable, ?>>(Parent.getClassCssMetaData());
			Collections.addAll(styleables,
					RIPPLER_FILL,
					MASK_TYPE
					);
			STYLEABLES = Collections.unmodifiableList(styleables);
		}
	}


	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return getClassCssMetaData();
	}
	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.STYLEABLES;
	}

	

}