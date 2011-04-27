package com.spaceprogram.simplejpa.cache;

import com.appoxy.kittycache.KittyCache;

/**
 * 
 * 
 * Wrapper for KittyCache: http://code.google.com/p/kitty-cache/
 * 
 */
public class KittyCacheWrapper extends KittyCache implements Cache {

    public KittyCacheWrapper(int i) {
        super(i);
    }

    @Override
    public Object getObj(Object o) {
        return get(o);
    }

}
