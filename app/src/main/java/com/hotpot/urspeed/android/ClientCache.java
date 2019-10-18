package com.hotpot.urspeed.android;

import com.hotpot.urspeed.android.model.Car;

public class ClientCache {
    private static Car CURRENT_CAR = null;

    public static Car getCurrentCar() {
        synchronized (ClientCache.class) {
            return CURRENT_CAR;
        }
    }

    public static void setCurrentCar(Car currentCar) {
        synchronized (ClientCache.class) {
            CURRENT_CAR = currentCar;
        }
    }
}
