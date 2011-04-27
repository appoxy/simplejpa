package com.spaceprogram.simplejpa.stats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds stats about the last operation performed. Useful for testing.
 * <p/>
 * User: treeder
 * Date: Apr 9, 2008
 * Time: 8:34:44 PM
 */
public class OpStats implements Statistics {
    private AtomicInteger puts = new AtomicInteger();
    private AtomicInteger putsDuration = new AtomicInteger();

    private AtomicInteger deletes = new AtomicInteger();
    private AtomicInteger attsDeleted = new AtomicInteger();
    private AtomicLong attsDeletedDuration = new AtomicLong();

    private AtomicInteger gets = new AtomicInteger();
    private AtomicLong getsDuration = new AtomicLong();

    private AtomicInteger s3Puts = new AtomicInteger();
    private AtomicLong s3PutsDuration = new AtomicLong();

    private AtomicInteger s3Gets = new AtomicInteger();
    private AtomicLong s3GetsDuration = new AtomicLong();

    private AtomicInteger attsPut = new AtomicInteger();
    private AtomicLong attsPutDuration = new AtomicLong();

    public AtomicInteger queries = new AtomicInteger();


    public void s3Put(long duration) {
        s3Puts.incrementAndGet();
        s3PutsDuration.addAndGet(duration);
    }

    public void s3Get(long duration) {
        s3Gets.incrementAndGet();
        s3GetsDuration.addAndGet(duration);
    }

    public void attsPut(int numAtts, long duration) {
        puts.incrementAndGet();
        attsPut.addAndGet(numAtts);
        attsPutDuration.addAndGet(duration);
    }

    public void attsDeleted(int attsDeleted, long duration) {
        deletes.incrementAndGet();
        this.attsDeleted.addAndGet(attsDeleted);
        attsDeletedDuration.addAndGet(duration);
    }

    public void got(int numItems, long duration2) {
        gets.addAndGet(numItems);
        getsDuration.addAndGet(duration2);
    }

    public int getPuts() {
        return puts.get();
    }

    public void setPuts(AtomicInteger puts) {
        this.puts = puts;
    }

    public int getPutsDuration() {
        return putsDuration.get();
    }

    public void setPutsDuration(AtomicInteger putsDuration) {
        this.putsDuration = putsDuration;
    }

    public int getS3Puts() {
        return s3Puts.get();
    }

    public void setS3Puts(AtomicInteger s3Puts) {
        this.s3Puts = s3Puts;
    }

    public long getS3PutsDuration() {
        return s3PutsDuration.get();
    }

    public void setS3PutsDuration(AtomicLong s3GetsDuration) {
        this.s3PutsDuration = s3PutsDuration;
    }

    public int getS3Gets() {
        return s3Gets.get();
    }

    public void setS3Gets(AtomicInteger s3Gets) {
        this.s3Gets = s3Gets;
    }

    public long getS3GetsDuration() {
        return s3GetsDuration.get();
    }

    public void setS3GetsDuration(AtomicLong s3GetsDuration) {
        this.s3GetsDuration = s3GetsDuration;
    }

    public int getAttsPut() {
        return attsPut.get();
    }

    public void setAttsPut(AtomicInteger attsPut) {
        this.attsPut = attsPut;
    }

    public long getAttsPutDuration() {
        return attsPutDuration.get();
    }

    public void setAttsPutDuration(AtomicLong attsPutDuration) {
        this.attsPutDuration = attsPutDuration;
    }

    public long getAttsDeletedDuration() {
        return attsDeletedDuration.get();
    }

    public void setAttsDeletedDuration(AtomicLong attsDeletedDuration) {
        this.attsDeletedDuration = attsDeletedDuration;
    }

    public int getAttsDeleted() {
        return attsDeleted.get();
    }

    public void setAttsDeleted(AtomicInteger attsDeleted) {
        this.attsDeleted = attsDeleted;
    }

    public int getDeletes(){
        return deletes.get();
    }

    public int getGets() {
        return gets.get();
    }

    public void setGets(AtomicInteger gets) {
        this.gets = gets;
    }

    public long getGetsDuration() {
        return getsDuration.get();
    }

    public void setGetsDuration(AtomicLong getsDuration) {
        this.getsDuration = getsDuration;
    }

    public int getQueries() {
        return queries.get();
    }

    public void setQueries(AtomicInteger queries) {
        this.queries = queries;
    }

    public void incrementGets() {
        this.gets.incrementAndGet();
    }

    @Override
    public String toString() {
        return "OpStats{" +
                "puts=" + puts +
                ", putsDuration=" + putsDuration +
                ", s3Puts=" + s3Puts +
                ", s3PutsDuration=" + s3PutsDuration +
                ", s3Gets=" + s3Gets +
                ", s3GetsDuration=" + s3GetsDuration +
                ", attsPut=" + attsPut +
                ", attsPutDuration=" + attsPutDuration +
                ", attsDeletedDuration=" + attsDeletedDuration +
                ", attsDeleted=" + attsDeleted +
                ", deletes=" + deletes +
                ", gets=" + gets +
                ", getsDuration=" + getsDuration +
                ", queries=" + queries +
                '}';
    }
}
