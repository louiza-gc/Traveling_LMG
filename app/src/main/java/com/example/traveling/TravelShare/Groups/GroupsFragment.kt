package com.example.traveling.TravelShare.Groups

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.traveling.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupsFragment : Fragment() {

    private lateinit var rvJoinedGroups: RecyclerView
    private lateinit var rvCreatedGroups: RecyclerView
    private lateinit var etSearchGroup: EditText
    private lateinit var btnCreateGroup: MaterialButton
    private lateinit var btnSearchGroup: MaterialButton
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvNoGroups: TextView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val joinedGroups = mutableListOf<GroupItem>()
    private val createdGroups = mutableListOf<GroupItem>()

    private val createGroupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            loadGroups()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvJoinedGroups = view.findViewById(R.id.rvJoinedGroups)
        rvCreatedGroups = view.findViewById(R.id.rvCreatedGroups)
        etSearchGroup = view.findViewById(R.id.etSearchGroup)
        btnCreateGroup = view.findViewById(R.id.btnCreateGroup)
        btnSearchGroup = view.findViewById(R.id.btnSearchGroup)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvNoGroups = view.findViewById(R.id.tvNoGroups)

        setupRecyclerViews()
        loadGroups()
        setupListeners()
    }

    private fun setupRecyclerViews() {
        rvJoinedGroups.layoutManager = LinearLayoutManager(requireContext())
        rvCreatedGroups.layoutManager = LinearLayoutManager(requireContext())
    }



    private fun loadGroups() {
        swipeRefresh.isRefreshing = true

        // 1. Récupérer tous les groupes
        firestore.collection("groups")
            .get()
            .addOnSuccessListener { groupsResult ->
                joinedGroups.clear()
                createdGroups.clear()

                // 2. Pour chaque groupe, vérifier si l'utilisateur est membre
                val pendingChecks = groupsResult.documents.map { doc ->
                    firestore.collection("groups")
                        .document(doc.id)
                        .collection("members")
                        .document(currentUserId)
                        .get()
                        .continueWith { task ->
                            Pair(doc, task.isSuccessful && task.result.exists())
                        }
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess<Pair<com.google.firebase.firestore.DocumentSnapshot, Boolean>>(pendingChecks)
                    .addOnSuccessListener { results ->
                        for ((doc, isMember) in results) {
                            if (isMember) {
                                val group = GroupItem(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "Sans nom",
                                    description = doc.getString("description") ?: "",
                                    memberCount = (doc.getLong("memberCount") ?: 0).toInt(),
                                    isMine = doc.getString("createdBy") == currentUserId,
                                    photoPath = doc.getString("photoPath") ?: ""
                                )

                                if (group.isMine) {
                                    createdGroups.add(group)
                                } else {
                                    joinedGroups.add(group)
                                }
                            }
                        }
                        updateUI()
                        swipeRefresh.isRefreshing = false
                    }
            }
            .addOnFailureListener { e ->
                swipeRefresh.isRefreshing = false
                Log.e("GroupsFragment", "Erreur: ${e.message}")
                Toast.makeText(requireContext(), "Erreur chargement: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI() {
        val joinedAdapter = GroupAdapter(joinedGroups) { group ->
            openGroupDetail(group)
        }
        rvJoinedGroups.adapter = joinedAdapter

        val createdAdapter = GroupAdapter(createdGroups) { group ->
            openGroupDetail(group)
        }
        rvCreatedGroups.adapter = createdAdapter

        tvNoGroups.visibility = if (joinedGroups.isEmpty() && createdGroups.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setupListeners() {
        btnCreateGroup.setOnClickListener {
            val intent = Intent(requireContext(), CreateGroupActivity::class.java)
            createGroupLauncher.launch(intent)
        }

        btnSearchGroup.setOnClickListener {
            showSearchGroupDialog()
        }

        swipeRefresh.setOnRefreshListener {
            loadGroups()
        }

        etSearchGroup.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchGroups(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun searchGroups(query: String) {
        if (query.isEmpty()) {
            updateUI()
            return
        }

        val allGroups = joinedGroups + createdGroups
        val filtered = allGroups.filter {
            it.name.lowercase().contains(query.lowercase())
        }

        val filteredJoined = filtered.filter { !it.isMine }
        val filteredCreated = filtered.filter { it.isMine }

        val joinedAdapter = GroupAdapter(filteredJoined) { group ->
            openGroupDetail(group)
        }
        rvJoinedGroups.adapter = joinedAdapter

        val createdAdapter = GroupAdapter(filteredCreated) { group ->
            openGroupDetail(group)
        }
        rvCreatedGroups.adapter = createdAdapter
    }

    private fun openGroupDetail(group: GroupItem) {
        val intent = Intent(requireContext(), GroupDetailsActivity::class.java)
        intent.putExtra("group_id", group.id)
        intent.putExtra("group_name", group.name)
        startActivity(intent)
    }

    // ==================== RECHERCHE DE GROUPES ====================

    private fun showSearchGroupDialog() {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_search_group)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        val etSearch = dialog.findViewById<EditText>(R.id.etSearchGroupQuery)
        val rvResults = dialog.findViewById<RecyclerView>(R.id.rvSearchResults)
        val tvNoResults = dialog.findViewById<TextView>(R.id.tvNoResults)
        val btnClose = dialog.findViewById<ImageButton>(R.id.btnCloseSearch)

        rvResults.layoutManager = LinearLayoutManager(requireContext())

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        val searchResults = mutableListOf<SearchGroupItem>()

        lateinit var adapter: SearchGroupAdapter

        adapter = SearchGroupAdapter(searchResults) { group, position ->
            joinGroup(group, position, searchResults, adapter, dialog)
        }
        rvResults.adapter = adapter

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchAllGroups(query, searchResults, adapter, tvNoResults)
                } else {
                    searchResults.clear()
                    adapter.notifyDataSetChanged()
                    tvNoResults.visibility = android.view.View.GONE
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialog.show()
    }


    private fun searchAllGroups(
        query: String,
        results: MutableList<SearchGroupItem>,
        adapter: SearchGroupAdapter,
        tvNoResults: TextView
    ) {
        // Récupérer tous les groupes depuis Firestore
        firestore.collection("groups")
            .get()
            .addOnSuccessListener { groupsResult ->
                results.clear()

                // Vérifier pour chaque groupe si l'utilisateur est membre
                val pendingChecks = groupsResult.documents.map { doc ->
                    firestore.collection("groups")
                        .document(doc.id)
                        .collection("members")
                        .document(currentUserId)
                        .get()
                        .continueWith { task ->
                            Pair(doc, task.isSuccessful && task.result.exists())
                        }
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess<Pair<com.google.firebase.firestore.DocumentSnapshot, Boolean>>(pendingChecks)
                    .addOnSuccessListener { checkResults ->
                        val myGroupIds = checkResults.filter { it.second }.map { it.first.id }.toSet()

                        for (doc in groupsResult) {
                            val groupName = doc.getString("name") ?: ""

                            if (groupName.lowercase().contains(query.lowercase())) {
                                val group = SearchGroupItem(
                                    id = doc.id,
                                    name = groupName,
                                    description = doc.getString("description") ?: "",
                                    memberCount = (doc.getLong("memberCount") ?: 0).toInt(),
                                    photoPath = doc.getString("photoPath") ?: "",
                                    isMember = myGroupIds.contains(doc.id)
                                )
                                results.add(group)
                            }
                        }

                        tvNoResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                        adapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { e ->
                        Log.e("GroupsFragment", "Erreur: ${e.message}")
                        tvNoResults.visibility = View.VISIBLE
                    }
            }
            .addOnFailureListener { e ->
                Log.e("GroupsFragment", "Erreur: ${e.message}")
                tvNoResults.visibility = View.VISIBLE
            }
    }

    private fun joinGroup(
        group: SearchGroupItem,
        position: Int,
        results: MutableList<SearchGroupItem>,
        adapter: SearchGroupAdapter,
        dialog: android.app.Dialog
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Ajouter dans la sous-collection members
        val memberData = hashMapOf(
            "userId" to currentUserId,
            "role" to "member",
            "joinedAt" to System.currentTimeMillis()
        )

        firestore.collection("groups")
            .document(group.id)
            .collection("members")
            .document(currentUserId)
            .set(memberData)
            .addOnSuccessListener {
                // Mettre à jour memberCount
                firestore.collection("groups")
                    .document(group.id)
                    .update("memberCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .addOnSuccessListener {
                        results[position] = group.copy(isMember = true)
                        adapter.notifyItemChanged(position)

                        Toast.makeText(requireContext(), "Vous avez rejoint ${group.name}", Toast.LENGTH_SHORT).show()
                        loadGroups()

                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            dialog.dismiss()
                        }, 1500)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erreur mise à jour: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}