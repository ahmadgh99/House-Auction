package com.example.auctionhouseapp.Fragments

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.auctionhouseapp.Activities.ItemsList
import com.example.auctionhouseapp.Activities.ViewItem
import com.example.auctionhouseapp.AuctionDayStatus
import com.example.auctionhouseapp.AuctionDays
import com.example.auctionhouseapp.Objects.Item
import com.example.auctionhouseapp.R
import com.example.auctionhouseapp.UserType
import com.example.auctionhouseapp.Utils.Constants
import com.example.auctionhouseapp.Utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuth

class HouseItemsList : Fragment() {

    var Day:AuctionDays = AuctionDays()
    var isRequestedList:Boolean = false
    val HouseId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var text_empty_items_list : TextView
    lateinit var ListView:ListView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_house_items_list, container, false)
        val Context = activity as ItemsList
        text_empty_items_list = view.findViewById<TextView>(R.id.textView_empty_items_list)
        text_empty_items_list.isVisible = false

        ListView = view.findViewById<ListView>(R.id.house_items_list)
        if(!isRequestedList) {
            view.findViewById<TextView>(R.id.textview_list_title).setText("Listed Items")
            if(Day.ListedItems.isEmpty())
                text_empty_items_list.isVisible = true
            ListView.adapter = CustomListAdapter(Context, Day.ListedItems)

        }
        else {
            view.findViewById<TextView>(R.id.textview_list_title).setText("Requested Items")
            if (Day.RequestedItems.isEmpty())
                text_empty_items_list.isVisible = true
            if (!Day.Status.equals(AuctionDayStatus.Pending)) {
                Day.RequestedItems.clear()
            }
                ListView.adapter = CustomListAdapter(Context, Day.RequestedItems)
        }


        ListView.setOnItemClickListener { parent, view, position, id ->
            val intent = Intent(Context, ViewItem::class.java)
            if(!isRequestedList) {
                var item = Day.ListedItems[position]
                intent.putExtra("Item", item)
                intent.putExtra("ListType",false)
            } else {
                var item = Day.RequestedItems[position]
                intent.putExtra("Item", item)
                intent.putExtra("ListType",true)
            }
            intent.putExtra("HouseID",HouseId)
            intent.putExtra("DayID", Day.DocumentID)
            intent.putExtra("SalesDate", Day.PrintDate())
            intent.putExtra("StartTime", Day.PrintStartTime())
            val userType = UserType.AuctionHouse.ordinal
            intent.putExtra("Type", userType)
            startActivity(intent)
            Context.finish()
        }

        view.findViewById<SwipeRefreshLayout>(R.id.swiperefresh).setOnRefreshListener {
            FirebaseUtils.houseCollectionRef
                .document(HouseId!!)
                .collection(Constants.SALES_DAY_COLLECTION)
                .document(Day.DocumentID)
                .get()
                .addOnSuccessListener {
                    view.findViewById<SwipeRefreshLayout>(R.id.swiperefresh).isRefreshing = false
                    Day = AuctionDays(it.data)
                    PerformAfterRefresh()
                }.addOnFailureListener {
                    Log.i("CustomerItemsList.kt", "Error! failed to refresh day")
                }
        }


        return view
    }

    fun PerformAfterRefresh() {
        if (!isRequestedList) {
            Day.FetchListedItems(HouseId!!, ::updateView, UserType.AuctionHouse)
        } else {
            Day.FetchRequestedItems(HouseId!!, ::updateView)
        }
    }

    private fun updateView () {
        this.requireView().findViewById<SwipeRefreshLayout>(R.id.swiperefresh).isRefreshing = false
        text_empty_items_list.isVisible = false
        if(!isRequestedList) {
            ListView.adapter = this.context?.let { CustomListAdapter(it,Day.ListedItems) }
            if(Day.ListedItems.isEmpty())
                text_empty_items_list.isVisible = true
        }

        else {
            ListView.adapter = this.context?.let { CustomListAdapter(it,Day.RequestedItems) }
            if(Day.RequestedItems.isEmpty())
                text_empty_items_list.isVisible = true
        }
    }


    private class CustomListAdapter(context: Context,items:ArrayList<Item>): BaseAdapter(){

        private val mContext:Context
        private val Items:ArrayList<Item>

        init{
            mContext = context
            Items = items
        }

        override fun getCount(): Int {
           return Items.size
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val View = layoutInflater.inflate(R.layout.house_item_list_item,parent,false)

            View.findViewById<TextView>(R.id.textview_house_item_name).setText(Items[position]._name)
            View.findViewById<TextView>(R.id.textView_description).setText(Items[position]._description)
            Glide.with(mContext)
                .load(Items[position]._imagesUrls.get(0))
                .into(View.findViewById<ImageView>(R.id.imageView_house_item))
            //View.findViewById<ImageView>(R.id.imageView_house_item).setImageBitmap(BitmapFactory.decodeByteArray(Items[position].ImagesArray[0],0,Items[position].ImagesArray[0].size))
            View.findViewById<ImageView>(R.id.imageView_house_item).setBackgroundResource(R.drawable.round_outline)
            View.findViewById<ImageView>(R.id.imageView_house_item).clipToOutline = true

            return View
        }
    }

}