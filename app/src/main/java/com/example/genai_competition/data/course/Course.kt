package com.example.genai_competition.data.course

data class Course(
    val id: String,
    val name: String,
    val academicYear: Int,
    val term: String,
    val pensum: List<String>,
    val description: String,
    val professor: String
)
