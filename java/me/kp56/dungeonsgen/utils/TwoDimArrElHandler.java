package me.kp56.dungeonsgen.utils;

@FunctionalInterface
public interface TwoDimArrElHandler<T> {
    boolean handle(int x, int y, T el);
}
