package org.swellrt.beta.testing;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.swellrt.beta.model.remote.STextRemote;
import org.swellrt.beta.model.remote.SubstrateId;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.wave.Blip;

public class FakePlatformBasedFactory implements PlatformBasedFactory {

  @Override
  public STextRemote getSTextRemote(SObjectRemote object, SubstrateId substrateId, Blip blip) {

    return new STextRemote(object, substrateId, blip) {

      @Override
      public String getRawContent() {
        return blip.getContent().toXmlString();
      }

      @Override
      public DocInitialization getInitContent() {
        return blip.getContent().toInitialization();
      }

      @Override
      public void setInitContent(DocInitialization ops) {
        throw new IllegalStateException("Not implemented");
      }

      @Override
      public boolean isEmpty() {
        return SUtils.isEmptyDocument(blip.getContent());
      }
      
    };
  }

}
