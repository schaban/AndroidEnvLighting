// Author: Sergey Chaban <sergey.chaban@gmail.com>

package flow;

import java.nio.ByteBuffer;

import geas.*;

import static android.opengl.GLES20.*;

public class GLGeoData extends GeoData {

	protected int[] mIdVBO;
	protected int[] mIdIBO;

	public int getVBONum() {
		return mIdVBO != null ? mIdVBO.length : 0;
	}

	public int getIBONum() {
		return mIdIBO != null ? mIdIBO.length : 0;
	}

	public void initGLBuffers() {
		int nbuf = 0;
		int nbat = getBatchesNum();
		if (isSharedVB()) {
			nbuf = 1 + nbat;
		} else {
			nbuf = nbat * 2;
		}
		int[] ids = new int[nbuf];
		glGenBuffers(nbuf, ids, 0);
		boolean ok = true;
		for (int i = 0; i < nbuf; ++i) {
			if (ids[i] == 0) {
				ok = false;
				break;
			}
		}
		if (!ok) {
			for (int i = 0; i < nbuf; ++i) {
				if (ids[i] != 0) {
					glDeleteBuffers(1, ids, i);
				}
			}
			return;
		}
		if (isSharedVB()) {
			mIdVBO = new int[1];
			mIdIBO = new int[nbat];
			mIdVBO[0] = ids[0];

			for (int i = 1; i < nbuf; ++i) {
				mIdIBO[i - 1] = ids[i];
			}

			glBindBuffer(GL_ARRAY_BUFFER, mIdVBO[0]);
			glBufferData(GL_ARRAY_BUFFER, getSharedVBByteSize(), getSharedVB(), GL_STATIC_DRAW);
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		} else {
			mIdVBO = new int[nbat];
			mIdIBO = new int[nbat];

			for (int i = 0; i < nbat; ++i) {
				mIdVBO[i] = ids[i];
			}
			for (int i = 0; i < nbat; ++i) {
				mIdIBO[i] = ids[nbat + i];
			}

			for (int i = 0; i < nbat; ++i) {
				glBindBuffer(GL_ARRAY_BUFFER, mIdVBO[i]);
				glBufferData(GL_ARRAY_BUFFER, mBatches[i].getPrivateVBByteSize(), mBatches[i].getVB(), GL_STATIC_DRAW);
			}
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}

		for (int i = 0; i < nbat; ++i) {
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mIdIBO[i]);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, mBatches[i].getIBByteSize(), mBatches[i].getIB(), GL_STATIC_DRAW);
		}
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	protected static int xlatElemType(ElemType t) {
		int glt = 0;
		switch (t) {
			case FLOAT:
				glt = GL_FLOAT;
				break;
			case USHORT:
				glt = GL_UNSIGNED_SHORT;
				break;
			case SHORT:
				glt = GL_SHORT;
				break;
			case UBYTE:
				glt = GL_UNSIGNED_BYTE;
				break;
			case BYTE:
				glt = GL_BYTE;
				break;
			case HALF:
				glt = 0x8D61; // GL_HALF_FLOAT_OES
				break;
		}
		return glt;
	}

	public void drawBatch(int id, AttrLink link) {
		if (!ckBatchId(id)) return;
		VtxInfo vi = getVtxInfo();
		int stride = vi.getByteSize();
		BatchInfo bat = mBatches[id];
		boolean useVBO = mIdVBO != null && mIdIBO != null;
		ByteBuffer batVB = null;
		int vbtop = bat.getMinIdx() * stride;
		if (useVBO) {
			if (isSharedVB()) {
				glBindBuffer(GL_ARRAY_BUFFER, mIdVBO[0]);
			} else {
				glBindBuffer(GL_ARRAY_BUFFER, mIdVBO[id]);
			}
		} else {
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			batVB = bat.getVB();
		}
		int nattrs = 0;
		int loc = link.mAttrLocPos;
		if (loc >= 0) {
			glEnableVertexAttribArray(loc);
			int offs = vbtop + vi.getPosOffs();
			int size = vi.getPosElemsNum();
			int type = xlatElemType(vi.getPosType());
			boolean nflg = type == GL_UNSIGNED_SHORT;
			if (useVBO) {
				glVertexAttribPointer(loc, size, type, nflg, stride, offs);
			} else {
				batVB.position(offs);
				glVertexAttribPointer(loc, size, type, nflg, stride, batVB);
			}
			++nattrs;
		}
		loc = link.mAttrLocNrm;
		if (loc >= 0) {
			glEnableVertexAttribArray(loc);
			int offs = vbtop + vi.getNrmOffs();
			int size = vi.getNrmElemsNum();
			int type = xlatElemType(vi.getNrmType());
			boolean nflg = type == GL_SHORT;
			if (useVBO) {
				glVertexAttribPointer(loc, size, type, nflg, stride, offs);
			} else {
				batVB.position(offs);
				glVertexAttribPointer(loc, size, type, nflg, stride, batVB);
			}
			++nattrs;
		}
		loc = link.mAttrLocTng;
		if (loc >= 0) {
			glEnableVertexAttribArray(loc);
			int offs = vbtop + vi.getTngOffs();
			int size = vi.getTngElemsNum();
			int type = xlatElemType(vi.getTngType());
			boolean nflg = type == GL_SHORT;
			if (useVBO) {
				glVertexAttribPointer(loc, size, type, nflg, stride, offs);
			} else {
				batVB.position(offs);
				glVertexAttribPointer(loc, size, type, nflg, stride, batVB);
			}
			++nattrs;
		}
		loc = link.mAttrLocTex;
		if (loc >= 0) {
			glEnableVertexAttribArray(loc);
			int offs = vbtop + vi.getTexOffs();
			int size = vi.getTexElemsNum();
			int type = xlatElemType(vi.getTexType());
			boolean nflg = false;
			if (useVBO) {
				glVertexAttribPointer(loc, size, type, nflg, stride, offs);
			} else {
				batVB.position(offs);
				glVertexAttribPointer(loc, size, type, nflg, stride, batVB);
			}
			++nattrs;
		}
		loc = link.mAttrLocClr;
		if (loc >= 0) {
			glEnableVertexAttribArray(loc);
			int offs = vbtop + vi.getClrOffs();
			int size = vi.getClrElemsNum();
			int type = xlatElemType(vi.getClrType());
			boolean nflg = false;
			if (useVBO) {
				glVertexAttribPointer(loc, size, type, nflg, stride, offs);
			} else {
				batVB.position(offs);
				glVertexAttribPointer(loc, size, type, nflg, stride, batVB);
			}
			++nattrs;
		}
		loc = link.mAttrLocJnt;
		if (loc >= 0) {
			glEnableVertexAttribArray(loc);
			int offs = vbtop + vi.getJntOffs();
			int size = 4;
			int type = xlatElemType(vi.getJntType());
			boolean nflg = false;
			if (useVBO) {
				glVertexAttribPointer(loc, size, type, nflg, stride, offs);
			} else {
				batVB.position(offs);
				glVertexAttribPointer(loc, size, type, nflg, stride, batVB);
			}
			++nattrs;
		}
		loc = link.mAttrLocWgt;
		if (loc >= 0) {
			glEnableVertexAttribArray(loc);
			int offs = vbtop + vi.getWgtOffs();
			int size = 4;
			int type = xlatElemType(vi.getWgtType());
			boolean nflg = true;
			if (useVBO) {
				glVertexAttribPointer(loc, size, type, nflg, stride, offs);
			} else {
				batVB.position(offs);
				glVertexAttribPointer(loc, size, type, nflg, stride, batVB);
			}
			++nattrs;
		}
		if (useVBO) {
			if (nattrs > 0) {
				glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, mIdIBO[id]);
				int icnt = bat.getTriNum() * 3;
				int ityp = bat.isIdx16() ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT;
				glDrawElements(GL_TRIANGLES, icnt, ityp, 0);
			}
			glBindBuffer(GL_ARRAY_BUFFER, 0);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		} else {
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
			if (nattrs > 0) {
				int icnt = bat.getTriNum() * 3;
				int ityp = bat.isIdx16() ? GL_UNSIGNED_SHORT : GL_UNSIGNED_INT;
				ByteBuffer batIB = bat.getIB();
				glDrawElements(GL_TRIANGLES, icnt, ityp, batIB);
			}
		}

		loc = link.mAttrLocPos;
		if (loc >= 0) glDisableVertexAttribArray(loc);
		loc = link.mAttrLocNrm;
		if (loc >= 0) glDisableVertexAttribArray(loc);
		loc = link.mAttrLocTng;
		if (loc >= 0) glDisableVertexAttribArray(loc);
		loc = link.mAttrLocTex;
		if (loc >= 0) glDisableVertexAttribArray(loc);
		loc = link.mAttrLocClr;
		if (loc >= 0) glDisableVertexAttribArray(loc);
		loc = link.mAttrLocJnt;
		if (loc >= 0) glDisableVertexAttribArray(loc);
		loc = link.mAttrLocWgt;
		if (loc >= 0) glDisableVertexAttribArray(loc);
	}

}
