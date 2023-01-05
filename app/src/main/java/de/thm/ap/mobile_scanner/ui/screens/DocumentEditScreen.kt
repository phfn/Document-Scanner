package de.thm.ap.mobile_scanner.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.CallSuper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import de.thm.ap.mobile_scanner.R
import de.thm.ap.mobile_scanner.data.AppDatabase
import de.thm.ap.mobile_scanner.model.Document
import de.thm.ap.mobile_scanner.model.DocumentImageRelation
import de.thm.ap.mobile_scanner.model.Tag
import de.thm.ap.mobile_scanner.model.Image
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class DocumentEditScreenViewModel(app: Application) : AndroidViewModel(app) {
  val dao = AppDatabase.getDb(app).documentDao()
  val tags: LiveData<List<Tag>> = dao.findAllTagsSync()
  var documentName: String by mutableStateOf("")
  val images: MutableList<Uri> = mutableStateListOf()
  var selectedTags: MutableList<Tag> = mutableStateListOf()


  fun saveDocument() {
    viewModelScope.launch() {
      val documentName = if (documentName.isNullOrEmpty()) null else documentName
      val documentId = dao.persist(Document(title = documentName))
      selectedTags.forEach { dao.persist(documentId, it.tagId!!) }
      val images_ids = images.map { dao.persist(Image(uri = it.toString())) }
      images_ids.forEach { dao.persist(DocumentImageRelation(documentId, it)) }
    }
  }
}

open class TakePicture : ActivityResultContract<Uri, Uri?>() {
  var uri: Uri? = null

  @CallSuper
  override fun createIntent(context: Context, input: Uri): Intent {
    uri = input
    return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
      .putExtra(MediaStore.EXTRA_OUTPUT, input)
  }

  final override fun getSynchronousResult(
    context: Context,
    input: Uri
  ): SynchronousResult<Uri?>? = null

  @Suppress("AutoBoxing")
  final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
    return if (resultCode == Activity.RESULT_OK) uri else null
  }
}


@Composable
fun DropDownItemMenuWithCheckbox(
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  DropdownMenuItem(
    onClick = { onClick() }, modifier = modifier
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(checked = selected, onCheckedChange = { onClick() })
      content()
    }
  }

}


@Composable
fun DocumentEditScreen(
  navController: NavController,
) {
  val vm: DocumentEditScreenViewModel = viewModel()
  val tags by vm.tags.observeAsState()

  var tagsExpanded by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier.fillMaxWidth(),
    topBar = { TopAppBar(title = { Text("Create Document") }) },
    content = { padding ->

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        ImageArea(vm.images)
        OutlinedTextField(
          value = vm.documentName,
          onValueChange = { vm.documentName = it },
          singleLine = true,
          label = {
            Text(text = "Document Title")
          },
          modifier = Modifier
            .padding(padding)
        )
        Box {
          OutlinedTextField(
            modifier = Modifier.clickable { tagsExpanded = true },
            enabled = false,
            value = vm.selectedTags.mapNotNull { it.name }.sorted().joinToString(),
            onValueChange = {},
            label = {
              Text(text = "Tags")
            },
            trailingIcon = {
              IconButton(onClick = { tagsExpanded = true }) {
                Icon(
                  imageVector = Icons.Default.ArrowDropDown,
                  contentDescription = "Select Tags"
                )
              }
            }

          )
          DropdownMenu(expanded = tagsExpanded, onDismissRequest = { tagsExpanded = false }) {
            tags?.filter { it.name != null }?.forEach { tag ->
              DropDownItemMenuWithCheckbox(
                modifier = Modifier.padding(padding),
                selected = vm.selectedTags.contains(tag),
                onClick = {
//                  vm.selectedTags = vm.selectedTags.also { if (it.contains(tag)) it.remove(tag) else it.add(tag) }
                  vm.selectedTags.apply { if (contains(tag)) remove(tag) else add(tag) }
                }) {
                Text(text = tag.name!!)
              }
            }
          }
        }

      }


    },
    floatingActionButton = {

      FloatingActionButton(
        onClick = {
          vm.saveDocument()
          navController.navigateUp()
        },
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Save Document"
        )
      }
    }
  )


}

@Composable
private fun ImageArea(
  images: MutableList<Uri>,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uuid by remember { mutableStateOf(UUID.randomUUID()) }
  val baseDir = File(context.filesDir, uuid.toString())
  val vm: DocumentEditScreenViewModel = viewModel()
  val getImageFromGalleryLauncher = rememberLauncherForActivityResult(contract = GetContent()) {
    if (it != null) images.add(it)
  }
  val getImageFromCameraLauncher = rememberLauncherForActivityResult(contract = TakePicture()) {
    if (it != null) images.add(it)
  }
  baseDir.mkdirs()
  Box(
    modifier = modifier
      .fillMaxWidth(1F)
      .fillMaxHeight(.3F)
  ) {
    LazyRow(modifier = Modifier.padding(16.dp)) {
      items(images) { uri ->
        AsyncImage(
          modifier = Modifier.padding(16.dp),
          model = uri,
          contentDescription = "Page ${images.indexOf(uri)}",
        )
      }
    }
    Column(
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.BottomEnd)
    ) {

      FloatingActionButton(
        modifier = Modifier
          .scale(0.8f)
          .alpha(0.8f),
        elevation = FloatingActionButtonDefaults.elevation(),
        onClick = { getImageFromGalleryLauncher.launch("image/*") },
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.add_photo_alternate),
          contentDescription = "Add Picture",
        )
      }
      FloatingActionButton(
        onClick = {
          val file = File(baseDir, "${images.size}.jpg")
          val uri = FileProvider.getUriForFile(context, "de.thm.fileprovider", file)
          getImageFromCameraLauncher.launch(uri)
        },
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(R.drawable.add_a_photo),
          contentDescription = "Add Picture",
        )
      }
    }
  }
}

