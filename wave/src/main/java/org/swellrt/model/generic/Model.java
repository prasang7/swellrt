package org.swellrt.model.generic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.swellrt.model.ReadableModel;
import org.swellrt.model.shared.ModelUtils;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.media.model.AttachmentIdGenerator;
import org.waveprotocol.wave.media.model.AttachmentIdGeneratorImpl;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Doc.T;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;
import org.waveprotocol.wave.model.wave.WaveletListener;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;
import org.waveprotocol.wave.model.waveref.WaveRef;

import com.google.common.collect.ImmutableMap;

/**
 * A model is a Wavelet wrapper storing a tree-like structure of data objects of
 * the Type hierarchy.
 *
 * <p>
 * [version not provided] <br/>
 * The original very buggy implementation. No longer supported.
 *
 * <p>
 * version 0.1 <br/>
 *
 * Each <code>Type</code> instance stores values in a new <code>Document</code>
 * but strings, they are stored in a separated document storing a string index.
 * <br/>
 *
 * Simplified <code>Type</code> interface, only one <code>attach()</code>
 * method. <br/>
 *
 * Improved class names to distinguish List and Map inner tools:
 * ListElementFactory...<br/>
 *
 * The main document is now "map+root", supporting metadata, index of string and
 * the root map.<br/>
 *
 * version 0.2 - SwellRT branding and new TextType<br/>
 *
 * Wave: s+XXXXXX <br/>
 * Wavelet: swl+root <br/>
 * Root Document : model+root <br/>
 *
 * version 1.0 - Data model supporting access control and metadata per blip
 *
 * Model metadata: model+root <br/>
 * Root map: map+root <br/>
 * Map blip: map+XXXX <br/>
 * List blip: list+XXXX <br/>
 * Text blip: b+XXXX <br/>
 *
 * Metadata attributes for model; <br/>
 * v = model version <br/>
 * t = model type id (for custom data types) <br/>
 * a = app id <br />
 *
 *
 * Metadata attributes for types (Map, List and future containers): <br/>
 * pc = participant creator <br/>
 * tc = timestamp creation <br/>
 * pm = participant lastmod <br/>
 * tm = timestamp lastmod <br/>
 * ap = access policy <br/>
 * acl = access control list <br/>
 * p = path of this object in the wavelet <br/>
 *
 * Primitive values are stored in each container document. (Former string index
 * is deprecated)
 *
 */
public class Model implements ReadableModel, SourcesEvents<Model.Listener> {

  /**
   * The model version of the current source code. Check {@link ModelMigrator}
   * for migration procedures.
   */
  public static final String MODEL_VERSION = "1.0";

  /**
   * For future use.
   */
  public static final String MODEL_TYPE_DEFAULT = "default";

  /**
   * For future use.
   */
  public static final String MODEL_APP_DEFAULT = "default";


  public interface Listener {

    void onAddParticipant(ParticipantId participant);

    void onRemoveParticipant(ParticipantId participant);

  }

  /**
   * A prefix for SwellRT wavelets.
   */
  public static final String WAVELET_SWELL_PREFIX = "swl";

  /**
   * Name of wavelet containing the public view (default) of a collaborative
   * object.
   */
  public static final String WAVELET_SWELL_ROOT = "swl+root";


  /**
   * Name of the blip/document storing collaborative object metadata
   */
  public static final String DOC_MODEL_ROOT = "model+root";


  /**
   * Name of substrate document for the root of the collaborative object.
   */
  public static final String DOC_MAP_ROOT = "map+root";

  /**
   * Tag name of the model section (metadata).
   */
  public static final String TAG_MODEL = "model";
  public static final String ATTR_VERSION_METADATA = "v";
  public static final String ATTR_TYPE_METADATA = "t";
  public static final String ATTR_APP_METADATA = "a";



  /**
   * Utility method to check for swellrt wavelet id.
   */
  public static boolean isModelWaveletId(WaveletId waveletId) {
    return waveletId.getId().startsWith(WAVELET_SWELL_PREFIX);
  }



  /**
   * The Document substrate of this object, the model metadata info of the
   * collaborative object.
   */
  private final ObservableDocument doc;

  /**
   * Wavelet supporting the whole collaborative object
   */
  private final ObservableWavelet wavelet;

  /**
   * Wavelet supporting the whole collaborative object
   */
  private final WaveletData waveletData;

  /**
   * An id generator for swellrt blips
   */
  private final TypeIdGenerator idGenerator;

  /**
   * An id generattor for attachments
   */
  private final AttachmentIdGenerator attachmentIdGenerator;

  /**
   * The current participant accesing the model
   */
  private final ParticipantId currentParticipant;

  /**
   * Reference to the root map
   */
  private MapType rootMap = null;


  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /**
   * Create or load a new collaborative object (aka model). Blip-based data will
   * be migrated to the current model version.
   *
   * @param wave
   * @param domain
   * @param loggedInUser
   * @param isNewWave
   * @param idGenerator
   * @return
   */
  public static Model create(ObservableWaveView wave, String domain, ParticipantId loggedInUser,
      boolean isNewWave, IdGenerator idGenerator) {

    WaveletId waveletId = WaveletId.of(domain, WAVELET_SWELL_ROOT);
    ObservableWavelet wavelet = wave.getWavelet(waveletId);

    // New
    if (wavelet == null) {
      wavelet = wave.createWavelet(waveletId);
      wavelet.addParticipant(loggedInUser);
    } else {

      // Existing, check for migration

      boolean wasOk = ModelMigrator.migrateIfNecessary(domain, wave);
      // TODO Log migration result
      if (!wasOk) return null;
    }

    //
    // Set up the Root document
    //
    ObservableDocument modelDocument = wavelet.getDocument(DOC_MODEL_ROOT);
    DocEventRouter router = DefaultDocEventRouter.create(modelDocument);

    //
    //
    // <model v="1.0" a="default" t="default"> </model>
    //
    Doc.E metadataElement = DocHelper.getElementWithTagName(modelDocument, TAG_MODEL);
    if (metadataElement == null) {

      metadataElement =
          modelDocument.createChildElement(modelDocument.getDocumentElement(), TAG_MODEL,
              ImmutableMap.of(ATTR_VERSION_METADATA, MODEL_VERSION, ATTR_TYPE_METADATA,
                  MODEL_TYPE_DEFAULT, ATTR_APP_METADATA, MODEL_APP_DEFAULT));
    }



    return new Model(wavelet, TypeIdGenerator.get(idGenerator), loggedInUser);

  }

  /**
   * Create a model from an existing wavelet. Don't perform integrity checks.
   * This method is intended for server-side logic.
   * 
   * @param wavelet the substrate of the object data model
   * @param participantId the user who will operates the object
   */
  public static Model create(ObservableWavelet wavelet, ParticipantId participantId, Seed seed) {

	  return new Model(wavelet,
			  TypeIdGenerator.get(new IdGeneratorImpl(participantId.getDomain(), seed)),
			  participantId);
  }


  /**
   * Constructor.
   *
   * @param wavelet
   * @param idGenerator
   */
  protected Model(ObservableWavelet wavelet, TypeIdGenerator idGenerator,
      ParticipantId currentParticipant) {

    this.wavelet = wavelet;
    this.wavelet.addListener(waveletListener);

    this.waveletData = wavelet.getWaveletData();
    this.idGenerator = idGenerator;
    this.attachmentIdGenerator =
        new AttachmentIdGeneratorImpl(idGenerator.getUnderlyingGenerator());

    this.doc = wavelet.getDocument(DOC_MODEL_ROOT);

    this.currentParticipant = currentParticipant;
  }

  public ParticipantId getCurrentParticipantId() {
    return currentParticipant;
  }

  public String getId() {
    return this.getWaveId().serialise();
  }

  public WaveId getWaveId() {
    return this.waveletData.getWaveId();
  }

  public WaveletId getWaveletId() {
    return wavelet.getId();
  }

  public WaveRef getWaveRef() {
    return WaveRef.of(getWaveId(), getWaveletId());
  }

  protected String generateDocId(String prefix) {
    return idGenerator.newDocumentId(prefix);
  }

  public AttachmentId generateAttachmentId() {
    return attachmentIdGenerator.newAttachmentId();
  }

  protected ObservableDocument createDocument(String docId) {
    Preconditions.checkArgument(!wavelet.getDocumentIds().contains(docId),
        "Trying to create an existing substrate document");
    return wavelet.getDocument(docId);
  }

  protected DocInitialization getTextDocInitialization(String text) {

    DocInitialization op;
    String initContent = "<body><line/>" + text + "</body>";

    try {
      op = DocProviders.POJO.parse(initContent).asOperation();
    } catch (IllegalArgumentException e) {
      if (e.getCause() instanceof XmlParseException) {
        // GWT.log("Ill-formed XML string ", e.getCause());
        // TODO How handle this?
      } else {
        // GWT.log("Error", e);
      }
      return null;
    }


    // DocumentSchema schema = ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS;

    // ViolationCollector vc = new ViolationCollector();
    // if (!DocOpValidator.validate(vc, schema, op).isValid()) {
    // GWT.log("That content does not conform to the schema: " + vc.toString());
    // return;
    // }

    return op;
  }

  protected Blip createBlip(String docId) {
    Preconditions.checkArgument(!wavelet.getDocumentIds().contains(docId),
        "Trying to create an existing substrate document");
    return wavelet.createBlip(docId);
  }

  protected ObservableDocument getDocument(String docId) {
    Preconditions.checkArgument(wavelet.getDocumentIds().contains(docId),
        "Trying to get a non existing substrate document");
    return wavelet.getDocument(docId);
  }


  protected Blip getBlip(String docId) {
    Preconditions.checkArgument(wavelet.getDocumentIds().contains(docId),
        "Trying to get a non existing substrate document");
    return wavelet.getBlip(docId);
  }


  //
  // Listeners
  //

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }


  @Override
  public void removeListener(Listener listener) {
    listeners.add(listener);
  }

  //
  // Public operations
  //

  public Set<ParticipantId> getParticipants() {
    return wavelet.getParticipantIds();
  }

  public void addParticipant(String address) {
    wavelet.addParticipant(ParticipantId.ofUnsafe(address));
  }

  public void removeParticipant(String address) {
    wavelet.removeParticipant(ParticipantId.ofUnsafe(address));
  }

  public MapType getRoot() {

    // Lazy initialization of the root map
    if (rootMap == null) {
      rootMap = MapType.deserialize(this, DOC_MAP_ROOT);
      if (rootMap.getPath().isEmpty()) rootMap.setPath("root");
    }

    return rootMap;
  }


  public MapType createMap() {
    return new MapType(this);
  }

  public StringType createString(String value) {
    return new StringType(value);
  }

  public ListType createList() {
    return new ListType(this);
  }

  public TextType createText() {
    return new TextType(this);
  }

  public TextType createText(String textOrXml) {
    TextType tt = new TextType(this);
    if (textOrXml != null) tt.setInitContent(textOrXml);
    return tt;
  }

  public FileType createFile(AttachmentId attachmentId) {
    return new FileType(attachmentId, null, this);
  }

  public FileType createFile(AttachmentId attachmentId, String contentType) {
    return new FileType(attachmentId, contentType, this);
  }

  public NumberType createNumber(String value) {
    return new NumberType(value);
  }

  public NumberType createNumber(int value) {
    return new NumberType(value);
  }

  public NumberType createNumber(double value) {
    return new NumberType(value);
  }

  public BooleanType createBoolean(boolean value) {
    return new BooleanType(value);
  }

  public BooleanType createBoolean(String value) {
    return new BooleanType(value);
  }

  @Override
  public Type fromPath(String path) {
    return (Type) ModelUtils.fromPath(this, path);
  }


  /**
   * For debug purposes only
   */
  public Set<String> getModelDocuments() {
    return wavelet.getDocumentIds();
  }

  /**
   * For debug purposes only
   */
  public String getModelDocument(String documentId) {
    return wavelet.getDocument(documentId).toXmlString();
  }

  private Map<String, DocumentHandler<Doc.N, Doc.E, Doc.T>> docHandlers =
      new HashMap<String, DocumentHandler<Doc.N, Doc.E, Doc.T>>();


  public void debugDocumentEvents(final String docId, boolean debug) {


    if (!wavelet.getDocumentIds().contains(docId)) return;

    ObservableDocument doc = getDocument(docId);

    if (debug) {

      if (!docHandlers.containsKey(docId)) {

        DocumentHandler<Doc.N, Doc.E, Doc.T> handler = new DocumentHandler<Doc.N, Doc.E, Doc.T>() {

          @Override
          public void onDocumentEvents(
              org.waveprotocol.wave.model.document.indexed.DocumentHandler.EventBundle<N, E, T> event) {

            ModelUtils.log("-------- (" + docId + ") --------");
            for (DocumentEvent<Doc.N, Doc.E, Doc.T> e : event.getEventComponents()) {
              ModelUtils.log(e.toString());
            }
            ModelUtils.log("----------------------------------");

          }
        };

        docHandlers.put(docId, handler);
        doc.addListener(handler);

      }

    } else {

      if (docHandlers.containsKey(docId)) docHandlers.remove(docId);

    }


  }


  //
  // Wavelet Listener
  //

  private final WaveletListener waveletListener = new WaveletListener() {

    @Override
    public void onParticipantRemoved(ObservableWavelet wavelet, ParticipantId participant) {
      for (Listener l : listeners)
        l.onRemoveParticipant(participant);
    }

    @Override
    public void onParticipantAdded(ObservableWavelet wavelet, ParticipantId participant) {
      for (Listener l : listeners)
        l.onAddParticipant(participant);
    }

    @Override
    public void onLastModifiedTimeChanged(ObservableWavelet wavelet, long oldTime, long newTime) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipAdded(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipRemoved(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipSubmitted(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipTimestampModified(ObservableWavelet wavelet, Blip blip, long oldTime,
        long newTime) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipVersionModified(ObservableWavelet wavelet, Blip blip, Long oldVersion,
        Long newVersion) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipContributorAdded(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipContributorRemoved(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onHashedVersionChanged(ObservableWavelet wavelet, HashedVersion oldHashedVersion,
        HashedVersion newHashedVersion) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }

  };

  public static Type getField(Type parent, String path) {

	  Preconditions.checkArgument(path != null, "Can't get field from null path");
	  Preconditions.checkArgument(parent != null, "Can't field from null parent");

	  if (path.isEmpty())
		  return parent;
	  
	  int pathSeparatorIndex = path.indexOf(".");	  
	  String keyOrIndex = pathSeparatorIndex != -1 ? path.substring(0, pathSeparatorIndex) : path;


	  if (pathSeparatorIndex == -1) {
		  return getFromContainerField(parent, keyOrIndex);		
	  } else {
		  String nextPath = path.substring(pathSeparatorIndex+1);
		  Type nextParent = getFromContainerField(parent, keyOrIndex);					
		  return getField(nextParent, nextPath);
	  }

  }
  
  
  private static Type getFromContainerField(Type container, String keyOrIndex) {
	  if (container instanceof MapType) {
		  MapType map = (MapType) container;
		  return map.get(keyOrIndex);
	  } else if (container instanceof ListType) {
		  try {
			  int index = Integer.valueOf(keyOrIndex);
			  ListType list = (ListType) container;
			  return list.get(index);
		  } catch (NumberFormatException e) {
		  }
	  }
	  return null;
  }

}
