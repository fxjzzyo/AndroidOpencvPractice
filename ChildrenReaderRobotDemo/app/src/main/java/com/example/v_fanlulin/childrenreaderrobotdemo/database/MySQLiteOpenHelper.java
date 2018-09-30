package com.example.v_fanlulin.childrenreaderrobotdemo.database;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.v_fanlulin.childrenreaderrobotdemo.MainActivity;

/**
 * Created by v_fanlulin on 2018/9/30.
 */

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

    //数据库版本号
    private static Integer version = 1;

    public MySQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public MySQLiteOpenHelper(Context context,String name, int version){
        this(context, name, null, version);
    }

    public MySQLiteOpenHelper(Context context, String name) {
        this(context,name,version);


    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建了数据库并创建一个叫records的表
        //SQLite数据创建支持的数据类型： 整型数据，字符串类型，日期类型，二进制的数据类型
        String sql = "create table "+ MainActivity.TABLE_NAME+"(picture_fingerprint text primary key, content text)";
        //execSQL用于执行SQL语句
        //完成数据库的创建
        db.execSQL(sql);
        //数据库实际上是没有被创建或者打开的，直到getWritableDatabase() 或者 getReadableDatabase() 方法中的一个被调用时才会进行创建或者打开

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println("更新数据库版本为:"+newVersion);
    }
}
