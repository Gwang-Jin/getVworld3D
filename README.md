# getVworld3D
브이월드 3d 데이터를 받아온다.

지형 설명은 이 곳 참고.
http://www.vw-lab.com/52

건물 및 교량 설명은 이 곳 참고.
http://www.vw-lab.com/53


----------------------------------
2019.04.03

독도는 별개의 layerName(=facility_dokdo)으로 다운로드 받아야 합니다. 레벨도 13으로 고정되어 있습니다.
DEM Crawler에서 받을 수 없고, building에 지형과 건물이 통합되어 있습니다. 해당 부분을 파일에 추가해 두었습니다.
독도는 dongdo.xdo와 seodo.xdo의 두 파일로 구성됩니다.
그런데 dongdo.xdo에서 읽어들이는 faceNum 이 -117로 되어 있어(알 수 없는 오류임), 이를 양수 117로 바꾸는 부분을 추가하였습니다.

기존에 vertexCount 가 음수로 되어 있어 에러가 났던 부분이 있습니다.
이 부분을 건너뛰도록 처리하여 문제가 없도록 했습니다.

데이터 파일이 3.0.0.2 버젼에서 실제로 여러개의 레이어가 포함된 경우, 파일 내에 포함된 저해상도 이미지를 읽지 않고 건너뛰면 에러가 나서, 해당 부분도 에러가 나지 않도록 처리했습니다.
