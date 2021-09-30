package com.example.room

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.room.databinding.ActivityMainBinding
import kotlinx.coroutines.runBlocking


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var myDao: MyDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myDao = MyDatabase.getDatabase(this).getMyDao()
        runBlocking {
            with(myDao) {
                insertStudent(Student(1, "james"))
                insertStudent(Student(2, "john"))
                insertClass(ClassInfo(1, "c-lang", "Mon 9:00", "E301", 1))
                insertClass(ClassInfo(2, "android prog", "Tue 9:00", "E302", 1))
                insertEnrollment(Enrollment(1, 1))
                insertEnrollment(Enrollment(1, 2))
            }
        }

        val allStudents = myDao.getAllStudents()
        allStudents.observe(this) {
            val str = StringBuilder().apply {
                    for ((id, name) in it) {
                        append(id)
                        append("-")
                        append(name)
                        append("\n")
                    }
                }.toString()
            binding.textStudentList.text = str
        }

        binding.queryStudent.setOnClickListener {
            val id = binding.editStudentId.text.toString().toInt()
            runBlocking {
                val results = myDao.getStudentsWithEnrollment(id)
                if (results.isNotEmpty()) {
                    val str = StringBuilder().apply {
                        append(results[0].student.id)
                        append("-")
                        append(results[0].student.name)
                        append(":")
                        for (c in results[0].enrollments) {
                            append(c.cid)
                            val cls_result = myDao.getClassInfo(c.cid)
                            if (cls_result.isNotEmpty())
                                append("(${cls_result[0].name})")
                            append(",")
                        }
                    }
                    binding.textQueryStudent.text = str
                } else {
                    binding.textQueryStudent.text = ""
                }
            }
        }
        binding.enroll.setOnClickListener{
            val id = binding.editStudentId.text.toString().toInt()
            runBlocking {
                val results = myDao.getStudentsWithEnrollment(id)
                val classResults= myDao.getClassInfo(1)
                myDao.insertEnrollment(Enrollment(id,1,"c-lang"))
                val str = StringBuilder().apply {
                    append(results[0].student.id)
                    append("-")
                    append(results[0].student.name)
                    append(":")
                    for (c in results[0].enrollments) {
                        append(c.cid)
                        val cls_result = myDao.getClassInfo(c.cid)
                        if (cls_result.isNotEmpty())
                            append("(${cls_result[0].name})")
                        append(",")
                    }
                }
                binding.textQueryStudent.text = str
            }
        }

        binding.delete.setOnClickListener{
            val id = binding.editStudentId.text.toString().toInt()
            runBlocking {
                val results = myDao.getStudentsWithEnrollment(id)
                val students =Student(id, results[0].student.name.toString())
                myDao.deleteStudent(students)
                //myDao.deleteByUserId(id)
            }
        }
        binding.addStudent.setOnClickListener {
            val id = binding.editStudentId.text.toString().toInt()
            val name = binding.editStudentName.text.toString()
            if (id > 0 && name.isNotEmpty()) {
                runBlocking {
                    myDao.insertStudent(Student(id, name))
                    //myDao.insertClass(ClassInfo(1, "c-lang", "Mon 9:00", "E301", 1))
                }
            }
        }

    }
}