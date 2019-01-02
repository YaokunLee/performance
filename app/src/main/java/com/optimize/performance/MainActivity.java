package com.optimize.performance;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.AsyncLayoutInflater;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.fastjson.JSON;
import com.optimize.performance.adapter.NewsAdapter;
import com.optimize.performance.adapter.OnFeedShowCallBack;
import com.optimize.performance.async.ThreadPoolUtils;
import com.optimize.performance.bean.NewsItem;
import com.optimize.performance.launchstarter.DelayInitDispatcher;
import com.optimize.performance.net.RetrofitNewsUtils;
import com.optimize.performance.tasks.delayinittask.DelayInitTaskA;
import com.optimize.performance.tasks.delayinittask.DelayInitTaskB;
import com.optimize.performance.utils.LaunchTimer;
import com.optimize.performance.utils.LogUtils;
import com.zhangyue.we.x2c.X2C;
import com.zhangyue.we.x2c.ano.Xml;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Xml(layouts = "activity_main")
public class MainActivity extends AppCompatActivity implements OnFeedShowCallBack {

    private RecyclerView mRecyclerView;
    private NewsAdapter mNewsAdapter;
    private String mStringIds = "20181122011549,20181122011548,20181122011543,20181122011542,20181122011541,20181122009017,20181122008536,20181122008166,20181122008095,20181122008092,20181122007961,20181122007960,20181122007450,20181122007447,20181122007207,20181122007205,20181122007218,20181122007217,20181122007216,20181122007208,20181122007206,20181122006459,20181122005903,20181122001699,20181121012358,20181121014438,20181121012359,20181121008591,20181121007797,20181121007019,20181121006484,20181121006150,20181121001543,20181120010835,20181120007348";

    private long mStartFrameTime = 0;
    private int mFrameCount = 0;
    private static final long MONITOR_INTERVAL = 160L; //单次计算FPS使用160毫秒
    private static final long MONITOR_INTERVAL_NANOS = MONITOR_INTERVAL * 1000L * 1000L;
    private static final long MAX_INTERVAL = 1000L; //设置计算fps的单位时间间隔1000ms,即fps/s;


    public List<NewsItem> mItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThreadPoolUtils.getService().execute(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                String oldName = Thread.currentThread().getName();
                Thread.currentThread().setName("new Name");
                LogUtils.i("");
                Thread.currentThread().setName(oldName);
            }
        });

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                LogUtils.i("Msg 执行");
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        });

        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {

                if (TextUtils.equals(name, "TextView")) {
                    // 生成自定义TextView
                }
                long time = System.currentTimeMillis();
                View view = getDelegate().createView(parent, name, context, attrs);
//                LogUtils.i(name + " cost " + (System.currentTimeMillis() - time));
                return view;
            }

            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                return null;
            }
        });

        new AsyncLayoutInflater(MainActivity.this).inflate(R.layout.activity_main, null, new AsyncLayoutInflater.OnInflateFinishedListener() {
            @Override
            public void onInflateFinished(@NonNull View view, int i, @Nullable ViewGroup viewGroup) {
                setContentView(view);
                mRecyclerView = findViewById(R.id.recycler_view);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                mRecyclerView.setAdapter(mNewsAdapter);
                mNewsAdapter.setOnFeedShowCallBack(MainActivity.this);
            }
        });

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        X2C.setContentView(MainActivity.this, R.layout.activity_main);
        mNewsAdapter = new NewsAdapter(mItems);

        getNews();
        getFPS();

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void getFPS() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (mStartFrameTime == 0) {
                    mStartFrameTime = frameTimeNanos;
                }
                long interval = frameTimeNanos - mStartFrameTime;
                if (interval > MONITOR_INTERVAL_NANOS) {
                    double fps = (((double) (mFrameCount * 1000L * 1000L)) / interval) * MAX_INTERVAL;
                    mFrameCount = 0;
                    mStartFrameTime = 0;
                } else {
                    ++mFrameCount;
                }

                Choreographer.getInstance().postFrameCallback(this);
            }
        });
    }

    private void getNews() {
        RetrofitNewsUtils.getApiService().getNBANews("banner", mStringIds)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String json = response.body().string();
                            JSONObject jsonObject = new JSONObject(json);
                            JSONObject data = jsonObject.getJSONObject("data");
                            Iterator<String> keys = data.keys();
                            while (keys.hasNext()) {
                                String next = keys.next();
                                JSONObject itemJO = data.getJSONObject(next);
                                NewsItem newsItem = JSON.parseObject(itemJO.toString(), NewsItem.class);
                                mItems.add(newsItem);
                            }
                            mNewsAdapter.setItems(mItems);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onFeedShow() {
//        new DispatchRunnable(new DelayInitTaskA()).run();
//        new DispatchRunnable(new DelayInitTaskB()).run();

        DelayInitDispatcher delayInitDispatcher = new DelayInitDispatcher();
        delayInitDispatcher.addTask(new DelayInitTaskA())
                .addTask(new DelayInitTaskB())
                .start();

        // 一系列操作 10s
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        LaunchTimer.endRecord("onWindowFocusChanged");
    }
}
