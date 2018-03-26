package com.github.khangtran.facebooklikeranking;

/**
 * Created by khang on 3/25/2018.
 */

public class Photo {
    public String createTime;
    public String name;
    public String id;
    public int totalCount;

    public Photo(String createTime, String name, String id) {
        this.createTime = createTime;
        this.name = name;
        this.id = id;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public String toString() {
        return "Photo{" +
                "createTime='" + createTime + '\'' +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", totalCount=" + totalCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Photo photo = (Photo) o;

        return id != null ? id.equals(photo.id) : photo.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
