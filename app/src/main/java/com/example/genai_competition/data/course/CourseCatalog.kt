package com.example.genai_competition.data.course

object CourseCatalog {
    val sampleStudent = StudentProfile(
        name = "Alex",
        program = "Computer Science",
        currentYear = 2
    )

    val courses: List<Course> = listOf(
        Course(
            id = "cs-algorithms",
            name = "Algorithms and Data Structures",
            academicYear = 2,
            term = "Fall",
            pensum = listOf(
                "Asymptotic analysis and algorithm complexity",
                "Sorting and searching algorithms",
                "Trees, heaps, and balanced search structures",
                "Graph traversals, shortest paths, and MST",
                "Dynamic programming and greedy approaches",
                "Introductory amortized analysis"
            ),
            description = "Core study of algorithm design techniques and performance analysis.",
            professor = "Dr. Elena Rodríguez"
        ),
        Course(
            id = "cs-systems",
            name = "Computer Systems",
            academicYear = 2,
            term = "Fall",
            pensum = listOf(
                "Instruction set architectures and assembly basics",
                "Memory hierarchy, caching strategies, and locality",
                "Processes, threads, and concurrency primitives",
                "Virtual memory, paging, and segmentation",
                "I/O systems and device management",
                "Performance profiling and optimization"
            ),
            description = "Explores how software interacts with hardware and operating systems.",
            professor = "Prof. Naomi Gupta"
        ),
        Course(
            id = "cs-theory",
            name = "Theory of Computation",
            academicYear = 2,
            term = "Spring",
            pensum = listOf(
                "Deterministic and non-deterministic finite automata",
                "Regular expressions and regular languages",
                "Context-free grammars and pushdown automata",
                "Turing machines and Church-Turing thesis",
                "Decidability, reducibility, and the halting problem",
                "Computational complexity and P vs NP"
            ),
            description = "Formal foundations of computation, languages, and complexity theory.",
            professor = "Dr. Matteo Rossi"
        )
    )
}
