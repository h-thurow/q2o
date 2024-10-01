package opix.domain.filetypes.filetypes;

import jakarta.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 18.05.18
 */
@Entity
@Table(name = "D_WIN_EXTENSION", schema = "dbo", catalog = "opixcntl")
public class WinExtensionEntity {
    private int fileTypeId;
    private String winExtension;
    private FileTypeEntity fileType;

    @Id
    @Column(name = "WE_EXTENSION")
    public String getWinExtension() {
        return winExtension;
    }

    public void setWinExtension(final String winExtension) {
        this.winExtension = winExtension;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final WinExtensionEntity that = (WinExtensionEntity) o;

        if (winExtension != null ? !winExtension.equals(that.winExtension) : that.winExtension != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return winExtension != null ? winExtension.hashCode() : 0;
    }

    @ManyToOne
    @JoinColumn(name = "WE_FTIDENT", referencedColumnName = "FT_IDENT", nullable = false)
    public FileTypeEntity getFileType() {
        return fileType;
    }

    public void setFileType(final FileTypeEntity fileType) {
        this.fileType = fileType;
        this.fileTypeId = fileType.getId();
    }

    @Override
    public String toString() {
        return "WinExtensionEntity{" +
                "fileTypeId=" + fileTypeId +
                ", winExtension='" + winExtension + '\'' +
                ", fileType=" + fileType +
                '}';
    }

    @Column(name = "WE_FTIDENT")
    public int getFileTypeId() {
        return fileTypeId;
    }

    public void setFileTypeId(int fileTypeId) {
        this.fileTypeId = fileTypeId;
    }
}
