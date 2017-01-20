package sergi.ivan.carles.artist;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import static java.lang.System.currentTimeMillis;


public class InitActivity extends AppCompatActivity {

    public static final int MILLIS_DAY = 86400000;
    public static final int MILLIS_HOUR = 3600000;
    public static final int MILLIS_MINUTE = 60000;
    public static final int NEW_EVENT = 0;
    public static final int UPDATE_EVENT = 1;
    public static final String REF_ARTIST_EVENT = "artist_events";
    public static final String REF_EVENTS = "events";
    public static final String REF_GROUPS = "groups";
    public static final String REF_SONGS = "songs";
    private ArrayList<Event> events;
    private ArrayList<Group> groups;
    private EventAdapter adapter;
    private DatabaseReference eventRef;
    private DatabaseReference artistEventRef;
    private DatabaseReference groupRef;
    private DatabaseReference artistRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        getSupportActionBar().setTitle(R.string.next_events);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        eventRef = database.getReference(REF_EVENTS);
        artistEventRef = database.getReference(REF_ARTIST_EVENT);
        groupRef = database.getReference(REF_GROUPS);
        artistRef = database.getReference();

        /*//Push some demo songs to firabase
        for(int i=0; i<20; i++){
            String id = songRef.push().getKey();
            String name = "song"+i;
            String artist = "artist"+i;
            songRef.child(id).child("name").setValue(name);
            songRef.child(id).child("artist").setValue(artist);
        }*/

        events = new ArrayList<>();
        groups = new ArrayList<>();
        adapter = new EventAdapter();

        loadArtist();

        ListView event_list = (ListView) findViewById(R.id.event_listView);
        event_list.setAdapter(adapter);

        event_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                onUpdateEvent(pos);
            }
        });

    }
    private void loadArtist(){
        artistRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot artistSnapshot) {
                for (DataSnapshot group : artistSnapshot.child(REF_GROUPS).getChildren()) {
                    String groupId = group.getKey();
                    String name = group.child("name").getValue().toString();
                    String[] songIds = new String[(int) group.child("songIds").getChildrenCount()];
                    int i = 0;
                    for (DataSnapshot songId : group.child("songIds").getChildren()) {
                        songIds[i] = songId.getValue().toString();
                        i++;
                    }
                    groups.add(new Group(groupId, name, songIds));
                }
                ArrayList<String> eventIds = new ArrayList<>();
                ArrayList<ArrayList<String>> eventGroupIds = new ArrayList<>();
                for(DataSnapshot event : artistSnapshot.child(REF_ARTIST_EVENT).getChildren()){
                    eventIds.add(event.getKey());
                    ArrayList<String> groupIds = new ArrayList<>();
                    if(event.hasChildren()){
                        for(DataSnapshot id : event.getChildren()){
                            groupIds.add(id.getValue().toString());
                        }
                    }
                    eventGroupIds.add(groupIds);
                }
                Query queryFutureEvents = eventRef.orderByChild("end").startAt(currentTimeMillis(), "end");
                loadArtistEvents(queryFutureEvents,eventIds,eventGroupIds);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadArtistEvents(final Query queryEvents, final ArrayList<String> eventIds, final ArrayList<ArrayList<String>> eventGroupIds) {
        queryEvents.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot eventSnapshot) {
                String id = eventSnapshot.getKey();
                for(int i=0; i<eventIds.size(); i++){
                    if(id.equals(eventIds.get(i))){
                        String name = eventSnapshot.child("name").getValue().toString();
                        String place = eventSnapshot.child("place").getValue().toString();
                        Long init = (long) eventSnapshot.child("start").getValue();
                        Long end = (long) eventSnapshot.child("end").getValue();
                        Event event = new Event(id,name, new Date(init), new Date(end), place);
                        if (eventSnapshot.child("room").exists()) {
                            event.setRoom(eventSnapshot.child("room").getValue().toString());
                        }
                        if(eventGroupIds.get(i).size() > 0){
                            event.setGroupIds(eventGroupIds.get(i));
                        }
                        events.add(event);
                        sortEvents();
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_init_activity, menu);
        return true;
    }

    private void onUpdateEvent(int pos) {
        Event event = events.get(pos);
        if(event.getStartDate().getTime() <= currentTimeMillis()){
            Intent intent = new Intent(this,ActualEventActivity.class);
            intent.putStringArrayListExtra("groupIds", event.getGroupIds());
            startActivity(intent);
        }
        else {
            Intent intent = new Intent(this, EventActivity.class);
            intent.putExtra("eventId", event.getId());
            intent.putExtra("name", event.getName());
            intent.putExtra("start", event.getStartDate().getTime());
            intent.putExtra("end", event.getEndDate().getTime());
            intent.putExtra("place", event.getPlace());
            String room = event.getRoom();
            if (room != null) {
                intent.putExtra("room", room);
            }
            ArrayList<String> groupIds = event.getGroupIds();
            if (groupIds != null) {
                intent.putExtra("groupIds", groupIds);
                ArrayList<String> groupNames = new ArrayList<>();
                for (int i = 0; i < groups.size(); i++) {
                    for (int j = 0; j < groupIds.size(); j++) {
                        if (groups.get(i).getId().equals(groupIds.get(j))) {
                            groupNames.add(groups.get(i).getName());
                        }
                    }
                }
                intent.putExtra("groupNames", groupNames);
            }
            startActivityForResult(intent, UPDATE_EVENT);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.op_new:
                Intent intent = new Intent(this, EventActivity.class);
                startActivityForResult(intent, NEW_EVENT);
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case NEW_EVENT:
                if(resultCode == RESULT_OK){
                    Log.i("info","New_event");
                    String id = eventRef.push().getKey();
                    String name = data.getStringExtra("name");
                    String place = data.getStringExtra("place");
                    Long init = data.getLongExtra("start", currentTimeMillis());
                    Long end = data.getLongExtra("end", currentTimeMillis());
                    Event event = new Event(id,name,new Date(init),new Date(end),place);

                    eventRef.child(id).child("start").setValue(init);
                    eventRef.child(id).child("place").setValue(place);
                    eventRef.child(id).child("name").setValue(name);
                    eventRef.child(id).child("end").setValue(end);

                    if(data.hasExtra("room")){
                        String room = data.getStringExtra("room");
                        eventRef.child(id).child("room").setValue(room);
                        event.setRoom(room);
                    }

                    if(data.hasExtra("groupIds")){
                        ArrayList<String> groupIds = data.getStringArrayListExtra("groupIds");
                        for(int i=0; i<groupIds.size(); i++){
                            artistEventRef.child(id).child("groupId"+String.valueOf(i)).setValue(groupIds.get(i));
                            groupRef.child(groupIds.get(i)).child("eventIds").child(id).setValue(true);
                        }
                        event.setGroupIds(groupIds);
                    }else{
                        artistEventRef.child(id).setValue(false);
                    }
                    events.add(event);
                    sortEvents();
                    adapter.notifyDataSetChanged();
                }
                else if(resultCode == RESULT_CANCELED) {
                    Log.i("info","New_event Discard");
                    if (data.hasExtra("groupIds")) {
                        ArrayList<String> groupIds = data.getStringArrayListExtra("groupIds");
                        for (int i = 0; i < groupIds.size(); i++) {
                            groupRef.child(groupIds.get(i)).removeValue();
                        }
                    }
                }
                break;
            case UPDATE_EVENT:
                Log.i("info", "Update_event");
                String eventId = data.getStringExtra("eventId");
                int pos=-1;
                for(int i=0; i<events.size(); i++) {
                    if (eventId.equals(events.get(i).getId())) {
                        pos=i;
                    }
                }
                if(pos==-1){Log.e("info", "UPDATE_EVENT, old event not found");}
                Event oldEvent = events.get(pos);

                if(resultCode == RESULT_OK){
                    Log.i("info", "Update save");
                    if(data.hasExtra("delete")){ //Case delete event
                        eventRef.child(eventId).removeValue();
                        artistEventRef.child(eventId).removeValue();

                        if (data.hasExtra("groupIds")) {
                            ArrayList<String> groupIds = data.getStringArrayListExtra("groupIds");
                            for (int i = 0; i < groupIds.size(); i++) {
                                groupRef.child(groupIds.get(i)).child("eventIds").child(eventId).removeValue();
                            }
                            //Delete events with no eventIds assigned
                            groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot groupsSnapshot) {
                                    for(DataSnapshot group : groupsSnapshot.getChildren()){
                                        if(!group.child("eventIds").exists()){
                                            groupRef.child(group.getKey()).removeValue();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                        }else{
                            if(oldEvent.getGroupIds() != null){
                                Log.e("info", "This case is not possible in the current version");
                                finish();
                            }
                        }
                        events.remove(pos);
                        adapter.notifyDataSetChanged();
                        break;
                    }

                    //Case update event
                    boolean changed = false;
                    String name = data.getStringExtra("name");
                    String place = data.getStringExtra("place");
                    Long init = data.getLongExtra("start", currentTimeMillis());
                    Long end = data.getLongExtra("end", currentTimeMillis());
                    Event updatedEvent = new Event(eventId, name, new Date(init), new Date(end), place);//Case update event

                    if(!oldEvent.getName().equals(updatedEvent.getName())){
                        changed=true;
                        eventRef.child(eventId).child("name").setValue(name);
                    }
                    if(!oldEvent.getPlace().equals(updatedEvent.getPlace())){
                        changed=true;
                        eventRef.child(eventId).child("place").setValue(place);
                    }
                    if(!oldEvent.getStartDate().equals(updatedEvent.getStartDate())){
                        changed=true;
                        eventRef.child(eventId).child("start").setValue(init);
                    }
                    if(!oldEvent.getEndDate().equals(updatedEvent.getEndDate())){
                        changed=true;
                        eventRef.child(eventId).child("end").setValue(end);
                    }

                    if (data.hasExtra("room")) {
                        String room = data.getStringExtra("room");
                        updatedEvent.setRoom(room);
                        if(!room.equals(oldEvent.getRoom())){
                            changed=true;
                            eventRef.child(eventId).child("room").setValue(room);
                        }
                    }else{
                        if(oldEvent.getRoom() != null){
                            changed=true;
                            eventRef.child(eventId).child("room").removeValue();
                        }
                    }
                    if (data.hasExtra("groupIds")) {
                        Log.i("info", "Update has Extra groupIds");
                        ArrayList<String> groupIds = data.getStringArrayListExtra("groupIds");
                        ArrayList<String> oldGroupIds = oldEvent.getGroupIds();
                        updatedEvent.setGroupIds(groupIds);
                        if(!groupIds.equals(oldGroupIds)){
                            changed=true;
                            if(oldGroupIds != null){
                                for (int i = 0; i < groupIds.size(); i++) {
                                    for(int j=0; j < oldGroupIds.size(); j++){
                                        if(groupIds.get(i).equals(oldGroupIds.get(j))){
                                            groupIds.set(i,"=");
                                            oldGroupIds.set(j,"=");
                                        }
                                    }
                                }
                                for(int j=0; j<oldGroupIds.size(); j++){
                                    if(!oldGroupIds.get(j).equals("=")){
                                        Log.e("info", "This case is not possible in the current version");
                                        finish();
                                    }
                                }
                                int num = oldGroupIds.size();
                                for(int i=0; i<groupIds.size(); i++){
                                    if(!groupIds.get(i).equals("=")){
                                        artistEventRef.child(eventId).child("groupId"+String.valueOf(num)).setValue(groupIds.get(i));
                                        num++;
                                    }
                                }
                            }else{
                                for(int i=0; i<groupIds.size(); i++){
                                    if(!groupIds.get(i).equals("=")){
                                        artistEventRef.child(eventId).child("groupId"+String.valueOf(i)).setValue(groupIds.get(i));
                                    }
                                }
                            }
                        }
                    }else{
                        if(oldEvent.getGroupIds() != null){
                            Log.e("info", "This case is not possible in the current version");
                            finish();
                        }
                    }

                    if(changed){ //If sth changed during the UPDATE, update list events
                        Log.i("info", "Update finish with changes");
                        events.set(pos, updatedEvent);
                        sortEvents();
                        adapter.notifyDataSetChanged();
                    }
                    break;

                }else if(resultCode == RESULT_CANCELED) {
                    Log.i("info", "Update discard");
                    if (data.hasExtra("groupIds")) {
                        ArrayList<String> groupIds = data.getStringArrayListExtra("groupIds");
                        ArrayList<String> oldGroupIds = new ArrayList<>(oldEvent.getGroupIds());
                        oldGroupIds.clear();
                        if(oldGroupIds.size()<0){
                            Log.i("info", "Update discard amb oldGroups != null");
                            for (int i = 0; i < groupIds.size(); i++) {
                                for(int j=0; j < oldGroupIds.size(); j++){
                                    if(groupIds.get(i).equals(oldGroupIds.get(j))){
                                        groupIds.set(i,"=");
                                        oldGroupIds.set(j,"=");
                                    }
                                }
                            }
                            for(int i=0; i<groupIds.size(); i++){
                                if(!groupIds.get(i).equals("=")){
                                    groupRef.child(groupIds.get(i)).child("eventIds").child(eventId).removeValue();
                                }
                            }
                            for(int j=0; j<oldGroupIds.size(); j++){
                                if(!oldGroupIds.get(j).equals("=")){
                                    Log.e("info", "This case is not possible in the current version");
                                    finish();
                                }
                            }
                        }else{
                            Log.i("info", "Update discard amb oldGroups == null");
                            for(int i=0; i<groupIds.size(); i++){
                                groupRef.child(groupIds.get(i)).child("eventIds").child(eventId).removeValue();
                            }
                        }


                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    private class EventAdapter extends ArrayAdapter<Event> {
        EventAdapter() {
            super(InitActivity.this, R.layout.item_events_list, events);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = convertView;
            if(convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                result = inflater.inflate(R.layout.item_events_list, parent, false);
            }
            Event event = getItem(position);
            TextView time = (TextView) result.findViewById(R.id.item_time);
            Date initDate = event.getStartDate();
            Long timeRemaining = initDate.getTime()-currentTimeMillis();
            if(timeRemaining<0){
                Date endDate = event.getEndDate();
                Long timeToEnd = endDate.getTime()-currentTimeMillis();
                if(timeToEnd>0){
                    time.setTextColor(getResources().getColor(R.color.colorAccent));
                    time.setText(R.string.now);
                }else{
                    Log.i("info", "Old group should not be here");
                    time.setText(R.string.old);
                }
            } else if(timeRemaining < MILLIS_HOUR){
                time.setText(String.valueOf(timeRemaining/ MILLIS_MINUTE)+" m");
            } else if(timeRemaining < MILLIS_DAY){
                time.setText(String.valueOf(timeRemaining/ MILLIS_HOUR)+" h");
            } else{
                time.setText(String.valueOf(timeRemaining/ MILLIS_DAY)+" d");
            }

            TextView name = (TextView) result.findViewById(R.id.item_name);
            name.setText(event.getName());

            TextView place = (TextView) result.findViewById(R.id.item_place);
            if(event.getRoom() != null){
                place.setText(event.getPlace() +" - "+event.getRoom());
            }else{
                place.setText(event.getPlace());
            }
            return result;
        }
    }

    private void sortEvents() {
        //Sort events by startDate
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                if (e1.getStartDate().after(e2.getStartDate())) return 1;
                else return -1;
            }
        });
    }

}
