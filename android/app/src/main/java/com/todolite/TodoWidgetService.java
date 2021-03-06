package com.todolite;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import com.facebook.react.modules.storage.ReactDatabaseSupplier;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.todolite.models.Content;
import com.todolite.models.Schedule;
import com.todolite.models.Todo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Frezc on 2016/6/20.
 */
public class TodoWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodoViewsFactory(this.getApplicationContext(), intent);
    }
}

class TodoViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    static final String TABLE_CATALYST = "catalystLocalStorage";
    static final String KEY_COLUMN = "key";
    static final String VALUE_COLUMN = "value";
    private static final int VIEW_TYPE_COUNT = 1;
    private Context context;
    private List<Todo> todolist = new ArrayList<>();

    public TodoViewsFactory(Context context, Intent intent) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        updateTodolist();
    }

    @Override
    public void onDataSetChanged() {
        updateTodolist();
    }

    private void updateTodolist() {
        SQLiteDatabase db = context.openOrCreateDatabase(ReactDatabaseSupplier.DATABASE_NAME,
                Context.MODE_PRIVATE, null);
        String schedule = queryFromDB(db, "schedule");
        String todos = queryFromDB(db, "todos");
        Gson gson = new Gson();
        todolist.clear();
        if (!schedule.isEmpty() && !todos.isEmpty()) {
            Schedule scheduleObj = gson.fromJson(schedule, Schedule.class);
            Log.i("Test", "updateTodolist: " + schedule);
            Map<String, Todo> todosMap = gson.fromJson(todos,
                    new TypeToken<Map<String, Todo>>(){}.getType());

            for (int todoId : scheduleObj.data) {
                Todo todo = todosMap.get(String.valueOf(todoId));
                if ("todo".equals(todo.status)) {
                    todolist.add(todo);
                }
            }
        }
    }

    private String queryFromDB(SQLiteDatabase db, String key) {
        // try-with-resource need min-api 19
        Cursor c = null;
        try {
            c = db.rawQuery("select * from " + TABLE_CATALYST + " where " + KEY_COLUMN + " = ?",
                    new String[]{key});
            if (c.getCount() > 0) {
                c.moveToFirst();
                String result = c.getString(c.getColumnIndex(VALUE_COLUMN));
                c.close();
                return result;
            }
        } catch (SQLiteException ignored) {
        } finally {
            if (c != null) c.close();
        }
        return "";
    }

    @Override
    public void onDestroy() {
        todolist.clear();
    }

    @Override
    public int getCount() {
        return todolist.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.todo_widget);
        Todo todo = todolist.get(position);
        views.setTextViewText(R.id.todo_title, todo.title);
        views.setImageViewResource(R.id.todo_typeicon, Constants.typeIcons.get(todo.type));
        List<String> descriptions = getDescriptions(position);

        switch (descriptions.size()) {
            case 0:
                views.setViewVisibility(R.id.todo_twoLinesDescription, View.GONE);
                views.setViewVisibility(R.id.todo_singleDescription, View.VISIBLE);
                views.setTextViewText(R.id.todo_singleDescription, "No description.");
                break;
            case 1:
                views.setViewVisibility(R.id.todo_twoLinesDescription, View.GONE);
                views.setViewVisibility(R.id.todo_singleDescription, View.VISIBLE);
                views.setTextViewText(R.id.todo_singleDescription, descriptions.get(0));
                break;
            default:
                views.setViewVisibility(R.id.todo_twoLinesDescription, View.VISIBLE);
                views.setViewVisibility(R.id.todo_singleDescription, View.GONE);
                views.setTextViewText(R.id.todo_description1, descriptions.get(0));
                views.setTextViewText(R.id.todo_description2, descriptions.get(1));
        }

        if (descriptions.size() > 0) {
            Log.i("todo", descriptions.get(0));
            views.setTextViewText(R.id.todo_description1, descriptions.get(0));
        } else {
            // 由于layout template里改变属性时会影响到其他项，所以这里必须重置。
            views.setTextViewText(R.id.todo_description1, "No description.");
        }
        if (descriptions.size() > 1) {
            Log.i("todo", descriptions.get(1));
            views.setTextViewText(R.id.todo_description1, descriptions.get(0));
            views.setTextViewText(R.id.todo_description2, descriptions.get(1));
        } else {
            views.setTextViewText(R.id.todo_description2, "");
        }

        Bundle extras = new Bundle();
        extras.putString(AppWidgetsEventReceiver.PAYLOAD,
                String.format(Locale.US, "{\"id\":%d}", todo.id));
        // 可以用来区分不同的事件
        extras.putString(AppWidgetsEventReceiver.ACTION, AppWidgetsModule.APPWIDGET_CLICK);
        Intent fillIntent = new Intent();
        fillIntent.putExtras(extras);
        fillIntent.setAction(AppWidgetsModule.APPWIDGET_CLICK);
        views.setOnClickFillInIntent(R.id.todo_widget, fillIntent);
        return views;
    }

    List<String> getDescriptions(int position) {
        Todo todo = todolist.get(position);

        Log.i("todo", String.valueOf(position));
        Log.i("todo", todo.toString());

        List<String> descriptions = new ArrayList<>();
        String time = constructTime(todo);
        if (!time.isEmpty()) {
            descriptions.add(time);
        }
        if (!todo.location.isEmpty()) {
            descriptions.add(todo.location);
        }
        String contents = constructContentsText(todo);
        if (!contents.isEmpty()) {
            descriptions.add(contents);
        }

        return descriptions;
    }

    String constructTime(Todo todo) {
        StringBuilder sb = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        int nowSec = (int) (calendar.getTimeInMillis() / 1000);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a");
        if (todo.deadline != 0) {
            if (nowSec < todo.deadline) {
                calendar.setTimeInMillis((long)todo.deadline * 1000);
                sb.append("Deadline: ").append(format.format(calendar.getTime()));
            } else {
                sb.append("Deadline has been exceeded! You must complete this todo!");
            }
        } else if (todo.startAt != 0) {
            if (nowSec < todo.startAt) {
                calendar.setTimeInMillis((long)todo.startAt * 1000);
                sb.append("Start at: ").append(format.format(calendar.getTime()));
            } else {
                sb.append("Started!! Care about your time!");
            }
        }

        return sb.toString();
    }

    String constructContentsText(Todo todo) {
        StringBuilder sb = new StringBuilder();
        int cl = todo.contents.size();
        List<Content> unComplete = new ArrayList<>();
        for (Content c : todo.contents) {
            if (c.status != 1) unComplete.add(c);
        }
        if (unComplete.size() > 1) {

            sb.append("Contents(")
              .append(cl - unComplete.size())
              .append(" / ")
              .append(cl).append("): ");
            for (int i = 0; i < unComplete.size(); i++) {
                sb.append(unComplete.get(i).content);
                if (i != unComplete.size() - 1) sb.append(", ");
            }
        } else if (unComplete.size() == 1) {
            sb.append("Content: ").append(unComplete.get(0).content);
        }
        return sb.toString();
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}