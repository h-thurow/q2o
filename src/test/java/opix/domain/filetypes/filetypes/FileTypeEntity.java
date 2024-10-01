package opix.domain.filetypes.filetypes;

import jakarta.persistence.*;
import java.util.Collection;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 18.05.18
 */
@Entity
@Table(name = "D_FILE_TYPES", schema = "dbo", catalog = "opixcntl")
public class FileTypeEntity {
    private int id;
    private String name;
    private String macType;
    private int layoutCapability;
    private int previewCapability;
    private int pdfCapability;
    private int fulltextCapability;
    private String createOrder;
    private Collection<WinExtensionEntity> winExtensions;

    @Id
    @Column(name = "FT_IDENT") @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "FT_NAME")
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "FT_MACTYPE")
    public String getMacType() {
        return macType;
    }

    public void setMacType(final String macType) {
        this.macType = macType;
    }

    @Basic
    @Column(name = "FT_LAYCAP")
    public int getLayoutCapability() {
        return layoutCapability;
    }

    public void setLayoutCapability(final int layoutCapability) {
        this.layoutCapability = layoutCapability;
    }

    @Basic
    @Column(name = "FT_PRVCAP")
    public int getPreviewCapability() {
        return previewCapability;
    }

    public void setPreviewCapability(final int previewCapability) {
        this.previewCapability = previewCapability;
    }

    @Basic
    @Column(name = "FT_PDFCAP")
    public int getPdfCapability() {
        return pdfCapability;
    }

    public void setPdfCapability(final int pdfCapability) {
        this.pdfCapability = pdfCapability;
    }

    @Basic
    @Column(name = "FT_FXTCAP")
    public int getFulltextCapability() {
        return fulltextCapability;
    }

    public void setFulltextCapability(final int fulltextCapability) {
        this.fulltextCapability = fulltextCapability;
    }

    @Basic
    @Column(name = "FT_PRVSEQ")
    public String getCreateOrder() {
        return createOrder;
    }

    public void setCreateOrder(final String createOrder) {
        this.createOrder = createOrder;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FileTypeEntity that = (FileTypeEntity) o;

        if (id != that.id) {
            return false;
        }
        if (layoutCapability != that.layoutCapability) {
            return false;
        }
        if (previewCapability != that.previewCapability) {
            return false;
        }
        if (pdfCapability != that.pdfCapability) {
            return false;
        }
        if (fulltextCapability != that.fulltextCapability) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (macType != null ? !macType.equals(that.macType) : that.macType != null) {
            return false;
        }
        if (createOrder != null ? !createOrder.equals(that.createOrder) : that.createOrder != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (macType != null ? macType.hashCode() : 0);
        result = 31 * result + layoutCapability;
        result = 31 * result + previewCapability;
        result = 31 * result + pdfCapability;
        result = 31 * result + fulltextCapability;
        result = 31 * result + (createOrder != null ? createOrder.hashCode() : 0);
        return result;
    }

    @OneToMany(mappedBy = "fileType")
    public Collection<WinExtensionEntity> getWinExtensions() {
        return winExtensions;
    }

    public void setWinExtensions(final Collection<WinExtensionEntity> winExtensions) {
        this.winExtensions = winExtensions;
    }

    @Override
    public String toString() {
        return "FileTypeEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", macType='" + macType + '\'' +
                ", layoutCapability=" + layoutCapability +
                ", previewCapability=" + previewCapability +
                ", pdfCapability=" + pdfCapability +
                ", fulltextCapability=" + fulltextCapability +
                ", createOrder='" + createOrder + '\'' +
                ", winExtensions=" + winExtensions +
                '}';
    }
}
