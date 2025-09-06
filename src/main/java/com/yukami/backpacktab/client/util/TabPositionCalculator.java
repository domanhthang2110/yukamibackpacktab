package com.yukami.backpacktab.client.util;

import com.yukami.backpacktab.client.config.TabConfig;

public class TabPositionCalculator {
    
    public static class TabLayout {
        public final int startX;
        public final int startY;
        public final boolean rightToLeft;
        
        public TabLayout(int startX, int startY, boolean rightToLeft) {
            this.startX = startX;
            this.startY = startY;
            this.rightToLeft = rightToLeft;
        }
        
        public int getTabX(int tabIndex, int tabSpacing) {
            if (rightToLeft) {
                return startX - (tabIndex * tabSpacing);
            } else {
                return startX + (tabIndex * tabSpacing);
            }
        }
    }
    
    public static TabLayout calculateLayout(TabConfig.TabPosition position, int screenLeft, int screenTop, 
                                          int screenWidth, int screenHeight, int tabWidth, int tabHeight) {
        
        return switch (position) {
            case TOP_LEFT -> new TabLayout(
                screenLeft, 
                screenTop - tabHeight + 4, 
                false
            );
            case TOP_RIGHT -> new TabLayout(
                screenLeft + screenWidth - tabWidth + 2, // Your custom offset
                screenTop - tabHeight + 4, 
                true
            );
            case BOTTOM_LEFT -> new TabLayout(
                screenLeft, 
                screenTop + screenHeight - 4, 
                false
            );
            case BOTTOM_RIGHT -> new TabLayout(
                screenLeft + screenWidth - tabWidth + 2, 
                screenTop + screenHeight - 4, 
                true
            );
        };
    }
}