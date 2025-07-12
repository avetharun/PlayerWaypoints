package dev.feintha.playerwaypoints;

import eu.pb4.polymer.virtualentity.api.elements.AbstractElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.MarkerElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.time.Instant;
import java.util.Optional;

public abstract class HologramData<T, E extends AbstractElement> {
    HologramData(T data) {
        this.data = data;
        this.element = createElement();
    }
    T data;
    E element;
    public abstract E createElement();

    public E getElement() { return element; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public abstract void updateElement();
    public Vec3d getPosition(Vec3d origin, int line) {
        return origin.offset(Direction.UP, (line * 0.25f));
    }
    public static abstract class DisplayHologramData<T, E extends DisplayElement> extends HologramData<T, E>{
        public DisplayEntity.BillboardMode billboardMode = DisplayEntity.BillboardMode.VERTICAL;
        public float shadowRadius = 0;
        public float shadowStrength = 1;
        DisplayHologramData(T data) {
            super(data);
        }

        @Override @MustBeInvokedByOverriders
        public void updateElement() {
            element.setBillboardMode(billboardMode);
            element.setShadowRadius(shadowRadius);
            element.setShadowStrength(shadowStrength);
        }
    }
    public static class TextDisplayHologramData extends DisplayHologramData<Text, TextDisplayElement> {
        TextDisplayHologramData(Text data) { super(data==null?Text.empty():data); }
        public boolean seeThrough;
        public int backgroundColor = 1073741824; // 0,0,0,0.25
        public DisplayEntity.TextDisplayEntity.TextAlignment textAlignment = DisplayEntity.TextDisplayEntity.TextAlignment.CENTER;
        public boolean shadow;
        public int lineWidth = Integer.MAX_VALUE;
        @Override
        public TextDisplayElement createElement() {
            return (element = new TextDisplayElement(data));
        }

        @Override
        public void updateElement() {
            super.updateElement();
            element.setText(data);
            element.setLineWidth(lineWidth);
            element.setShadow(shadow);
            element.setTextAlignment(textAlignment);
            element.setBackground(backgroundColor);
            element.setSeeThrough(seeThrough);
        }
    }
    public static class MarkerHologramData extends HologramData<Text, MarkerElement> {
        MarkerHologramData(Text data) { super(data); }

        @Override
        public MarkerElement createElement() {
            element = new MarkerElement();
            element.getDataTracker().set(EntityTrackedData.CUSTOM_NAME, Optional.ofNullable(data));
            element.getDataTracker().set(EntityTrackedData.NAME_VISIBLE, true);
            element.setInvisible(true);
            return element;
        }

        @Override
        public Vec3d getPosition(Vec3d origin, int line) {
            return super.getPosition(origin, line).offset(Direction.DOWN, 0.25f);
        }

        @Override
        public void updateElement() {
            element.getDataTracker().set(EntityTrackedData.CUSTOM_NAME, Optional.ofNullable(data));
            element.getDataTracker().set(EntityTrackedData.NAME_VISIBLE, true);
            element.setInvisible(true);

        }
    }
}