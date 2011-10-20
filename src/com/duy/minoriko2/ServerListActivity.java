/**
 * Copyright (c) 2011 Duy Truong <hduyudh@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.duy.minoriko2;

import com.duy.minoriko2.R;
import com.duy.minoriko2.control.Helper;
import com.duy.minoriko2.model.Server;
import com.duy.minoriko2.model.Servers;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ServerListActivity extends Activity {
    private ListView lv;
    private Servers serversDB;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        lv = new ListView(this);
        setContentView(lv);
        setTitle("Select server");
        serversDB = new Servers(this);
        lv.setAdapter(new ServersAdapter(serversDB.getAll()));
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v,
                    int position, long id) {
                Helper.setServerRoot(ServerListActivity.this,
                        ((Server) lv.getItemAtPosition(position)).addr,
                        ((Server) lv.getItemAtPosition(position)).type);
                finish();
            }
        });
//        registerForContextMenu(lv);
    }

//    @Override
//    public void onCreateContextMenu (ContextMenu menu, View v,
//            ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.serverscontext, menu);
//    }

//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//        AdapterContextMenuInfo info = (AdapterContextMenuInfo)
//                item.getMenuInfo();
//        switch (item.getItemId()) {
//        case R.id.serverscontext_del:
//            serversDB.delete(info.id);
//            ((ServersAdapter) lv.getAdapter()).setServers(serversDB.getAll());
//            ((ServersAdapter) lv.getAdapter()).notifyDataSetChanged();
//            return true;
//
//        default:
//            return super.onContextItemSelected(item);
//        }
//    }

//   @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.serverlist, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//        case R.id.serverlist_menu_add:
//            View v = View.inflate(this, R.layout.server_add_popup, null);
//            final Spinner typeC = (Spinner)
//                    v.findViewById(R.id.server_add_type);
//            final EditText addrC = (EditText)
//                    v.findViewById(R.id.server_add_addr);
//
//            ArrayAdapter<Server.Type> adapter =
//                    new ArrayAdapter<Server.Type>(this,
//                    android.R.layout.simple_spinner_item,
//                    Server.Type.values());
//            typeC.setAdapter(adapter);
//            adapter.setDropDownViewResource(
//                    android.R.layout.simple_spinner_dropdown_item);
//
//            Dialog dialog = (new AlertDialog.Builder(this))
//                    .setView(v)
//                    .setTitle("Add new server")
//                    .setPositiveButton("Add", new OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dia, int which) {
//                            String addr = addrC.getText().toString();
//                            if (serversDB.add(
//                                    (Server.Type) typeC.getSelectedItem(),
//                                    Helper.baseUrlCheck(addr))) {
//                                ((ServersAdapter) lv.getAdapter())
//                                        .setServers(serversDB.getAll());
//                                ((ServersAdapter) lv.getAdapter())
//                                        .notifyDataSetChanged();
//                            } else {
//                                Toast.makeText(ServerListActivity.this,
//                                        "Server already exist",
//                                        Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    })
//                    .setNegativeButton("Cancel", new OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dia, int which) {
//                            //dia.dismiss();
//                        }
//                    })
//                    .create();
//            dialog.show();
//            return true;
//
//        default:
//            return super.onOptionsItemSelected(item);
//        }
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        serversDB.close();
    }

    private class ServersAdapter extends BaseAdapter {
        private Server[] servers;

        public ServersAdapter(Server[] servers) {
            this.servers = servers;
        }

//        public void setServers(Server[] servers) {
//            this.servers = servers;
//        }

        @Override
        public int getCount() {
            if (servers != null)
                return servers.length;
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (servers != null)
                return servers[position];
            return "";
        }

        @Override
        public long getItemId(int position) {
            if (servers != null)
                return servers[position].id;
            return -1;
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            View row = v;
            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(
                        ServerListActivity.this);
                row = inflater.inflate(R.layout.serverlistrow, parent, false);
            }

            TextView topText = (TextView)
                    row.findViewById(R.id.serverlistrow_toptext);
            TextView bottomText = (TextView)
                    row.findViewById(R.id.serverlistrow_bottomtext);
            ImageView im = (ImageView)
                    row.findViewById(R.id.serverlistrow_img);

            if (Helper.getServerRoot(ServerListActivity.this)
                    .equals(servers[position].addr)) {
                im.setVisibility(View.VISIBLE);
            } else {
                im.setVisibility(View.INVISIBLE);
            }

            topText.setText(servers[position].addr);
            bottomText.setText(servers[position].type.toString());

            return row;
        }

    }
}
